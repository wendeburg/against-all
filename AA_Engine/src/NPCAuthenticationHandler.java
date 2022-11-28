import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import Game.Game;
import Game.Jugador;
import Utils.RandomTokenGenerator;

public class NPCAuthenticationHandler extends Thread {
    private String idPartida;
    private HashMap<String, Jugador> NPCs;
    private final String ipBroker;
    private final int puertoBroker;
    private KafkaConsumer<String, String> authRequestConsumer;
    private KafkaProducer<String, String> tokenOfferProducer;
    private boolean stopThread;
    private RandomTokenGenerator tokenGenerator;
    private Game partida;

    public void stopThread() {
        stopThread = true;
    }

    public NPCAuthenticationHandler(String idPartida, String ipBroker, int puertoBroker, HashMap<String, Jugador> NPCs, RandomTokenGenerator tokenGenerator, Game partida) {
        this.idPartida = idPartida;
        this.NPCs = NPCs;
        this.partida = partida;
        this.ipBroker = ipBroker;
        this.puertoBroker = puertoBroker;
        this.stopThread = false;
        this.tokenGenerator = tokenGenerator;
    }

    private void inicializarConsumidor() {
        Properties p = new Properties();
        p.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ipBroker + ":" + puertoBroker);
        p.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "aaengine-" + idPartida);
        p.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        p.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.toString(true));
        p.setProperty("security.protocol", "SSL");
        p.setProperty("ssl.truststore.location", "./secrets/all.truststore.jks");
        p.setProperty("ssl.truststore.password", "against-all-truststore-password");
        p.setProperty("ssl.endpoint.identification.algorithm", "");
        p.setProperty("ssl.keystore.location", "./secrets/engine.keystore.jks");
        p.setProperty("ssl.keystore.password", "against-all-aa-engine-password");
        p.setProperty("ssl.key.password", "against-all-aa-engine-password");

        authRequestConsumer = new KafkaConsumer<>(p);
        authRequestConsumer.subscribe(Arrays.asList("NPCAUTHREQUEST"));
    }

    private void inicializarProductor() {
        Properties p = new Properties();
        p.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ipBroker + ":" + puertoBroker);
        p.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.setProperty("security.protocol", "SSL");
        p.setProperty("ssl.truststore.location", "./secrets/all.truststore.jks");
        p.setProperty("ssl.truststore.password", "against-all-truststore-password");
        p.setProperty("ssl.endpoint.identification.algorithm", "");
        p.setProperty("ssl.keystore.location", "./secrets/engine.keystore.jks");
        p.setProperty("ssl.keystore.password", "against-all-aa-engine-password");
        p.setProperty("ssl.key.password", "against-all-aa-engine-password");

        tokenOfferProducer = new KafkaProducer<>(p);
    }

    private ArrayList<String> getDisconectedNPCs(HashMap<String, Long> offersLogger) {
        ArrayList<String> npcsDesconectados = new ArrayList<>();

        for (String npcID : offersLogger.keySet()) {
            long t = System.currentTimeMillis() / 1000;

            // Los npcs que no hayan contestado al offer en más de 10 segundos
            // se consideran desconectados.
            if (t - offersLogger.get(npcID) > 10) {
                npcsDesconectados.add(npcID);
            }
        }

        return npcsDesconectados;
    }

    private void removeDisconnectedNPCs(ArrayList<String> npcsDesconectados, HashMap<String, Long> offersLogger) {
        for (String npcID : npcsDesconectados) {
            System.out.println("El npc " + npcID + " no ha contestado a ninguna oferta de token. Probablemente se haya perdido la conexión.");
            offersLogger.remove(npcID);
        }
    }

    @Override
    public void run() {
        inicializarConsumidor();
        inicializarProductor();

        JSONParser parser = new JSONParser();
        // Guarda el id del npc y la hora cuando hizo la petición.
        HashMap<String, Long> offersLogger = new HashMap<>();

        while (!stopThread) {
            ConsumerRecords<String, String> peticiones = authRequestConsumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, String> peticion : peticiones) {
                try {
                    JSONObject peticionJSON = (JSONObject) parser.parse(peticion.value().toString());

                    if (peticionJSON.get("type").equals("request")) {
                        String idNPC = peticionJSON.get("npcid").toString();
                    
                        JSONObject obj = new JSONObject();
                        obj.put("npcid", idNPC);
                        obj.put("token", tokenGenerator.generarToken());
                        obj.put("partida", idPartida);
    
                        tokenOfferProducer.send(new ProducerRecord<String,String>("TOKENOFFERS", obj.toString()));

                        offersLogger.put(idNPC, System.currentTimeMillis() / 1000);
                    }
                    else if (peticionJSON.get("type").equals("offer-accepted")) {
                        String idNPC = peticionJSON.get("npcid").toString();

                        offersLogger.remove(idNPC);

                        String idPartidaAceptada = peticionJSON.get("partida").toString();

                        if (idPartidaAceptada.equals(idPartida)) {
                            int nivelNPC = Integer.parseInt(peticionJSON.get("nivel").toString());
                            int efNPC = Integer.parseInt(peticionJSON.get("ef").toString());
                            int ecNPC = Integer.parseInt(peticionJSON.get("ec").toString());
                            int tokenNPC = Integer.parseInt(peticionJSON.get("token").toString());
                            
                            Jugador npc = new Jugador(nivelNPC, tokenNPC, idNPC, efNPC, ecNPC);
                            npc.setAsNPC();

                            NPCs.put(idNPC, npc);
                            partida.addPlayerToMap(npc);
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            } 

            removeDisconnectedNPCs(getDisconectedNPCs(offersLogger), offersLogger);
        }
    }
}
