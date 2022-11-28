import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.bson.Document;

import com.mongodb.client.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import Game.Ciudad;
import Game.Coordenada;
import Game.Jugador;
import Game.Game;
import Utils.RandomTokenGenerator;

public class GameHandler extends Thread {
    private final AuthenticationHandler authThread;
    private final String ipBroker;
    private final int puertoBroker;
    private final String archivoCiudades;
    private final String archivoGuardarEstadoPartida;
    private final boolean partidaRecuperada;
    private RandomTokenGenerator tokenGenerator;
    private KafkaConsumer<String, String> playerMovementsConsumer;
    private KafkaProducer<String, String> mapProducer;
    private ArrayList<Ciudad> ciudades;
    private HashMap<String, Jugador> jugadores;
    private HashMap<String, Jugador> NPCs;
    private Game partida;
    private String idPartida;
    private final MongoCollection<Document> coleccionUsuarios;
    private final MongoCollection<Document> coleccionMapa;
    private final MongoClient cliente;

    public GameHandler(String ipBroker, int puertoBroker, String archivoGuardarEstadoPartida, JSONObject estadoPartida, RandomTokenGenerator tokenGenerator, String ipDB, int puertoDB) throws Exception {
        this.authThread = null;
        this.ipBroker = ipBroker;
        this.puertoBroker = puertoBroker;
        this.archivoCiudades = "";
        this.archivoGuardarEstadoPartida = archivoGuardarEstadoPartida;
        this.partidaRecuperada = true;
        this.tokenGenerator = tokenGenerator;

        this.idPartida = estadoPartida.get("idpartida").toString();

        initPlayers((JSONObject) estadoPartida.get("jugadores"));
        initNPCs((JSONObject) estadoPartida.get("npcs"));
        initCities((JSONObject) estadoPartida.get("ciudades"));

        cliente = MongoClients.create("mongodb://" + ipDB + ":" + puertoDB);
        MongoDatabase db = cliente.getDatabase("against-all-db");
        coleccionUsuarios = db.getCollection("users");
        coleccionMapa = db.getCollection("latest-map");

        this.partida = new Game((JSONArray) estadoPartida.get("mapa"), jugadores, NPCs, ciudades, coleccionUsuarios);
    }

    private void initPlayers(JSONObject jugadoresJSON) throws NumberFormatException {
        this.jugadores = new HashMap<>();

        for (Object k : jugadoresJSON.keySet()) {
            String key = k.toString();
            Integer nivel = Integer.parseInt(((JSONObject) jugadoresJSON.get(key)).get("nivel").toString());
            Integer token = Integer.parseInt(((JSONObject) jugadoresJSON.get(key)).get("token").toString());
            Integer ef = Integer.parseInt(((JSONObject) jugadoresJSON.get(key)).get("efectoFrio").toString());
            Integer ec = Integer.parseInt(((JSONObject) jugadoresJSON.get(key)).get("efectoCalor").toString());
            JSONArray pos = (JSONArray) ((JSONObject) jugadoresJSON.get(key)).get("posicion");

            Jugador jugador = new Jugador(nivel, token, key, ef, ec);
            jugador.setPosicion(new Coordenada(Integer.parseInt(pos.get(0).toString()), Integer.parseInt(pos.get(1).toString())));

            tokenGenerator.addTokenToUsedTokens(token);

            jugadores.put(key, jugador);
        }
    }

    private void initNPCs(JSONObject NPCsJSON) throws NumberFormatException {
        this.NPCs = new HashMap<>();

        for (Object k : NPCsJSON.keySet()) {
            String key = k.toString();
            Integer nivel = Integer.parseInt(((JSONObject) NPCsJSON.get(key)).get("nivel").toString());
            Integer token = Integer.parseInt(((JSONObject) NPCsJSON.get(key)).get("token").toString());
            Integer ef = Integer.parseInt(((JSONObject) NPCsJSON.get(key)).get("efectoFrio").toString());
            Integer ec = Integer.parseInt(((JSONObject) NPCsJSON.get(key)).get("efectoCalor").toString());
            JSONArray pos = (JSONArray) ((JSONObject) NPCsJSON.get(key)).get("posicion");

            Jugador jugador = new Jugador(nivel, token, key, ef, ec);
            jugador.setPosicion(new Coordenada(Integer.parseInt(pos.get(0).toString()), Integer.parseInt(pos.get(1).toString())));

            jugador.setAsNPC();

            tokenGenerator.addTokenToUsedTokens(token);

            NPCs.put(key, jugador);
        }
    }

    private void initCities(JSONObject ciudadesJSON) throws NumberFormatException {
        this.ciudades = new ArrayList<>();

        for (Object c : ciudadesJSON.keySet()) {
            String nombre = c.toString();
            Float temperatura = Float.parseFloat(ciudadesJSON.get(nombre).toString());

            Ciudad ciudad = new Ciudad(nombre, temperatura);

            ciudades.add(ciudad);
        }
    }

    public GameHandler(AuthenticationHandler authThread, String ipBroker, int puertoBroker, String archivoCiudades, String archivoGuardarEstadoPartida, RandomTokenGenerator tokenGenerator, String ipDB, int puertoDB) {
        this.partidaRecuperada = false;
        this.authThread = authThread;
        this.ipBroker = ipBroker;
        this.puertoBroker = puertoBroker;
        this.archivoCiudades = archivoCiudades;
        this.archivoGuardarEstadoPartida = archivoGuardarEstadoPartida;
        this.tokenGenerator = tokenGenerator;
        this.NPCs = new HashMap<>();

        this.idPartida = UUID.randomUUID().toString();

        this.ciudades = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ciudades.add(new Ciudad());
        }

        cliente = MongoClients.create("mongodb://" + ipDB + ":" + puertoDB);
        MongoDatabase db = cliente.getDatabase("against-all-db");
        coleccionUsuarios = db.getCollection("users");
        coleccionMapa = db.getCollection("latest-map");

        this.partida = new Game(NPCs, coleccionUsuarios);
    }

    private String leeSocket(Socket socketCliente) throws IOException {
        DataInputStream dis = new DataInputStream(socketCliente.getInputStream());
        return dis.readUTF();
    }

    private void escribeSocket(String mensaje, Socket socketCliente) throws IOException {
        DataOutputStream dos = new DataOutputStream(socketCliente.getOutputStream());
        dos.writeUTF(mensaje);
    }

    private JSONObject mandarPeticion(String nombreCiduad) {
        try {
            URL enlacePeticion = new URL("https://api.openweathermap.org/data/2.5/weather?q=" + nombreCiduad + "&appid=96e1dac69045891ae2002db2c6a5f6cc&units=metric");
        
            HttpURLConnection conn = (HttpURLConnection) enlacePeticion.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
        
            if (conn.getResponseCode() != 200) {
                return new JSONObject();
            }
            else {
                String resStr = "";
                Scanner scanner = new Scanner(enlacePeticion.openStream());
            
                while (scanner.hasNext()) {
                    resStr += scanner.nextLine();
                }
                scanner.close();
            
                JSONParser parse = new JSONParser();
                return (JSONObject) parse.parse(resStr);
            }
        }
        catch (Exception e) {
            System.out.println("Ha habido un error al hacer la request a OpenWeather.");
            return new JSONObject();
        }
    }

    public void guardarCiudad(String nombreCiudad, float temp, int numeroCiudades) {
        ciudades.get(numeroCiudades).setNombre(nombreCiudad);
        ciudades.get(numeroCiudades).setTemperatura(temp);
    }

    private ArrayList<String> obtenerCiudades() throws FileNotFoundException {
        ArrayList<String> nombreCiudades = new ArrayList<>();

        File archivo = new File(archivoCiudades);
        Scanner reader = new Scanner(archivo);

        while (reader.hasNextLine()) {
            String nombreCiudad = reader.nextLine();
            nombreCiudades.add(nombreCiudad);
        }
        reader.close();

        return nombreCiudades;
    }

    private void obtenerTemperaturas() throws FileNotFoundException {
        ArrayList<String> nombreCiudades = obtenerCiudades();
        int temperaturasObtenidas = 0;
    
        for (String nombreCiudad : nombreCiudades) {
            if (temperaturasObtenidas >= 4) {
                // Ya se tienen todas las temperaturas necesarias.
                break;
            }

            JSONObject respuestaJSON = mandarPeticion(nombreCiudad);

            if (respuestaJSON.size() == 0) {
                // La ciudad no existe o ha ocurrido un error y no hay temperatura.
                continue; 
            }
            else {
                float temp = Float.parseFloat(((JSONObject) respuestaJSON.get("main")).get("temp").toString());

                guardarCiudad(nombreCiudad, temp, temperaturasObtenidas);
                temperaturasObtenidas++;
            }
        }

        if (temperaturasObtenidas < 4) {
            System.out.println("No se pudieron obtener 4 ciudades. Algunas tendrán valores por defecto.");
        }
    }

    private void inicializarConsumidorDeMovimientosDeJugadores() {
        Properties p = new Properties();
        p.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ipBroker + ":" + puertoBroker);
        p.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "aaengine-" + UUID.randomUUID().toString());
        p.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        p.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.toString(true));
        p.setProperty("security.protocol", "SSL");
        p.setProperty("ssl.truststore.location", "./secrets/all.truststore.jks");
        p.setProperty("ssl.truststore.password", "against-all-truststore-password");
        p.setProperty("ssl.endpoint.identification.algorithm", "");
        p.setProperty("ssl.keystore.location", "./secrets/engine.keystore.jks");
        p.setProperty("ssl.keystore.password", "against-all-aa-engine-password");
        p.setProperty("ssl.key.password", "against-all-aa-engine-password");

        playerMovementsConsumer = new KafkaConsumer<>(p);
        playerMovementsConsumer.subscribe(Arrays.asList("PLAYERMOVEMENTS"));
    }

    private void inicializarProductorDeMapa() {
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

        mapProducer = new KafkaProducer<>(p);
    }

    private Jugador getPlayer(int token) {
        for (String alias : jugadores.keySet()) {
            if (jugadores.get(alias).getToken() == token) {
                return jugadores.get(alias);
            }
        }

        for (String id : NPCs.keySet()) {
            if (NPCs.get(id).getToken() == token) {
                return NPCs.get(id);
            }
        }

        return null;
    }

    private void initLastMovementsLogger(HashMap<Integer, Long> logger, long tiempoIncial) {
        for (String alias : jugadores.keySet()) {
            logger.put(jugadores.get(alias).getToken(), tiempoIncial);
        }
    }

    private ArrayList<Jugador> getDisconectedPlayers(HashMap<Integer, Long> lastMovementsLogger) {
        ArrayList<Jugador> jugadoresDesconectados = new ArrayList<>();

        for (Integer token : lastMovementsLogger.keySet()) {
            Jugador j = getPlayer(token);

            if (j != null) {
                long t = System.currentTimeMillis() / 1000;

                // Los jugadores que no hayan hecho un movimiento por más de 10 segundos
                // se consideran desconectados.
                if (t - lastMovementsLogger.get(token) > 10) {
                    jugadoresDesconectados.add(j);
                }
            }
        }

        return jugadoresDesconectados;
    }

    private void removeDisconnectedPlayers(ArrayList<Jugador> jugadoresDesconectados, HashMap<Integer, Long> lastMovementsLogger) {
        for (Jugador j : jugadoresDesconectados) {
            System.out.println("El jugador/npc " + j.getAlias() + " no ha hecho un movimiento por más de 10 segundos. Ha sido removido de la partida.");
            lastMovementsLogger.remove(j.getToken());
            jugadores.remove(j.getAlias());
            partida.removePlayerFromMap(j);

            if (!j.getIsNPC()) {
                coleccionUsuarios.updateOne(new Document("alias", j.getAlias()), new Document("$set", new Document("nivel", j.getNivel())));
            }
        }
    }

    private String getGameStateAsJSONString(ArrayList<String> ganadores) {
        JSONObject obj = partida.toJSONObject();

        if (ganadores == null) {
            obj.put("gamefinished", false);
            obj.put("winners", new JSONArray());
        }
        else {
            obj.put("gamefinished", true);
            JSONArray winners = new JSONArray();

            for (String s : ganadores) {
                winners.add(s);
            }
            
            obj.put("winners", winners);
        }

        return obj.toJSONString();
    }

    private JSONObject getNPCsAsJSONObject() {
        JSONObject obj = new JSONObject();

        for (String alias : NPCs.keySet()) {
            Jugador j = NPCs.get(alias);
            JSONObject jugador = new JSONObject();

            jugador.put("nivel", j.getNivel());
            jugador.put("posicion", j.getPosicion().toJSONArray());
            jugador.put("token", j.getToken());
            jugador.put("isNPC", new Boolean(j.getIsNPC()).toString());
            jugador.put("efectoFrio", j.getEfectoFrio());
            jugador.put("efectoCalor", j.getEfectoCalor());

            obj.put(alias, jugador);
        }

        return obj;
    }

    private JSONObject getPlayersAsJSONObject() {
        JSONObject obj = new JSONObject();

        for (String alias : jugadores.keySet()) {
            Jugador j = jugadores.get(alias);
            JSONObject jugador = new JSONObject();

            jugador.put("nivel", j.getNivel());
            jugador.put("posicion", j.getPosicion().toJSONArray());
            jugador.put("token", j.getToken());
            jugador.put("isNPC", new Boolean(j.getIsNPC()).toString());
            jugador.put("efectoFrio", j.getEfectoFrio());
            jugador.put("efectoCalor", j.getEfectoCalor());

            obj.put(alias, jugador);
        }

        return obj;
    }

    private void saveGameState(ArrayList<String> ganadores) {
        JSONArray mapaJSON = partida.getMapAsJSONArray();
        JSONObject jugadoresJSON = getPlayersAsJSONObject();
        JSONObject npcsJSON = getNPCsAsJSONObject();
        JSONObject citiesJSON = partida.getCiudadesAsJSONObject();
        
        JSONObject obj = new JSONObject();
        obj.put("idpartida", idPartida);
        obj.put("mapa", mapaJSON);
        obj.put("jugadores", jugadoresJSON);
        obj.put("npcs", npcsJSON);
        obj.put("ciudades", citiesJSON);

        if (ganadores == null) {
            obj.put("gamefinished", false);
            obj.put("winners", new JSONArray());
        }
        else {
            obj.put("gamefinished", true);
            JSONArray winners = new JSONArray();

            for (String s : ganadores) {
                winners.add(s);
            }
            
            obj.put("winners", winners);
        }

        // Se guarda la partida en un archivo.
        PrintWriter writer;
        try {
            writer = new PrintWriter(archivoGuardarEstadoPartida);

            writer.print("");
            writer.print(obj.toJSONString());
            writer.close();
        } catch (FileNotFoundException e) {
            System.out.println("No se ha podido abrir el archivo para guardar la partida.");
        }

        // Se guarda en al DB para la API.
        coleccionMapa.insertOne(Document.parse(obj.toJSONString()));
    }

    private void gestionarPartida() {
        JSONParser parser = new JSONParser();
        HashMap<Integer, Long> lastMovementsLogger = new HashMap<>();

        mapProducer.send(new ProducerRecord<String,String>("MAP", getGameStateAsJSONString(null)));
        saveGameState(null);

        long tiempoInicial = System.currentTimeMillis() / 1000;
        initLastMovementsLogger(lastMovementsLogger, tiempoInicial);
        // La partida termina en 4 minutos o cuando quede un jugador.
        while (jugadores.size() > 1 && ((System.currentTimeMillis() / 1000) - tiempoInicial) <= 240) {
            long tickControl = System.currentTimeMillis();
            
            ConsumerRecords<String, String> movimientos = playerMovementsConsumer.poll(Duration.ofMillis(100));
            
            for (ConsumerRecord<String, String> movimiento : movimientos) {
                try {
                    JSONObject movimientoJSON = (JSONObject) parser.parse(movimiento.value().toString());
                    String tokenJugador = new ArrayList<String>(movimientoJSON.keySet()).get(0).toString();
                    String m = movimientoJSON.get(tokenJugador).toString();
                    
                    Jugador j = getPlayer(Integer.parseInt(tokenJugador));

                    if (j == null) {
                        continue;
                    }

                    // Si el movimiento no es un Keeapalive realizar movimiento.
                    if (!m.equals("KA")) {
                        partida.movePlayer(m, j);
                    }
                    
                    lastMovementsLogger.put(j.getToken(), System.currentTimeMillis() / 1000);
                } catch (Exception e) {
                    continue;
                }
            } 

            removeDisconnectedPlayers(getDisconectedPlayers(lastMovementsLogger), lastMovementsLogger);

            long timeElapsed = System.currentTimeMillis() - tickControl;

            if (timeElapsed < 500) {
                try {
                    sleep(500 - timeElapsed);
                } catch (InterruptedException e) {
                    System.out.println("Could not put the GameHandler thread to sleep.");
                }
            }

            mapProducer.send(new ProducerRecord<String,String>("MAP", getGameStateAsJSONString(null)));
            mapProducer.flush();
            saveGameState(null);
        }
    }

    @Override
    public void run() {
        NPCAuthenticationHandler npcAuthHandler = new NPCAuthenticationHandler(idPartida, ipBroker, puertoBroker, NPCs, tokenGenerator, partida);
        npcAuthHandler.start();

        if (!partidaRecuperada) {
            try {
                obtenerTemperaturas();
            } catch (FileNotFoundException e1) {
                System.out.println("No se ha encontrado el archivo para leer las ciudades. Usando ciudades por defecto.");
            }
    
            partida.setCiudades(ciudades);
        }

        try {

            if (!partidaRecuperada) {
                authThread.join();

                jugadores = authThread.getJugadores();
                partida.setJugadores(jugadores);
            }

            inicializarConsumidorDeMovimientosDeJugadores();
            inicializarProductorDeMapa();
            
            System.out.println("La partida con id >>> " + idPartida + " <<< ha comenzado.");
            gestionarPartida();
            System.out.println("La partida con id >>> " + idPartida + " <<< ha finalizado.");

            ArrayList<String> ganadores = new ArrayList<>();

            if (jugadores.size() == 1) {
                System.out.println("El jugador >>> " + jugadores.get(jugadores.keySet().toArray()[0]).getAlias() + " <<< ha ganado.");
                ganadores.add(jugadores.get(jugadores.keySet().toArray()[0]).getAlias());
                coleccionUsuarios.updateOne(new Document("alias", jugadores.get(jugadores.keySet().toArray()[0]).getAlias()), new Document("$set", new Document("nivel", jugadores.get(jugadores.keySet().toArray()[0]).getNivel())));
            }
            else {
                Jugador jugadorConMayorNivel = null;

                for (String j : jugadores.keySet()) {
                    Jugador currentPlayer = jugadores.get(j);

                    if (jugadorConMayorNivel == null || jugadorConMayorNivel.getNivel() < currentPlayer.getNivel()) {
                        jugadorConMayorNivel = currentPlayer;
                    }

                    coleccionUsuarios.updateOne(new Document("alias", currentPlayer.getAlias()), new Document("$set", new Document("nivel", currentPlayer.getNivel())));
                }

                ArrayList<Jugador> jugadoresConMayorNivel = new ArrayList<>();

                for (String j : jugadores.keySet()) {
                    Jugador currentPlayer = jugadores.get(j);

                    if (jugadorConMayorNivel.getNivel() == currentPlayer.getNivel() && jugadorConMayorNivel.getToken() != currentPlayer.getToken()) {
                        jugadoresConMayorNivel.add(currentPlayer);
                    }
                }

                if (jugadoresConMayorNivel.size() == 0) {
                    System.out.println("El jugador >>> " + jugadorConMayorNivel.getAlias() + " <<< ha ganado.");
                    ganadores.add(jugadorConMayorNivel.getAlias());
                }
                else {
                    System.out.println("Ha habido un empate entre los jugadores: ");
                    System.out.println(">>> " + jugadorConMayorNivel.getAlias() + " <<<");

                    for (Jugador j : jugadoresConMayorNivel) {
                        System.out.println(">>> " + j.getAlias() + " <<<");
                        ganadores.add(j.getAlias());
                    }
                }
            }

            mapProducer.send(new ProducerRecord<String,String>("MAP", getGameStateAsJSONString(ganadores)));
            saveGameState(ganadores);

            npcAuthHandler.stopThread();
            playerMovementsConsumer.close();
            mapProducer.close();
            cliente.close();
        } catch (InterruptedException e) {
            System.out.println("No se puede esperar al hilo de autenticación porque se ha interrumpido.");
        }
    }
}
