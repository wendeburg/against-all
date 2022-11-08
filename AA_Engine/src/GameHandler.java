import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import Game.Ciudad;
import Game.Jugador;
import Game.Game;
import Utils.MessageParser;
import Utils.MessageParserException;

public class GameHandler extends Thread {
    private final AuthenticationHandler authThread;
    private final String ipBroker;
    private final int puertoBroker;
    private final String ipServidorClima;
    private final int puertoServidorClima;
    private final String archivoCiudades;
    private KafkaConsumer<String, String> playerMovementsConsumer;
    private KafkaProducer<String, String> mapProducer;
    private ArrayList<Ciudad> ciudades;
    private HashMap<String, Jugador> jugadores;
    private Game partida;
    private String idPartida;

    public GameHandler(AuthenticationHandler authThread, String ipBroker, int puertoBroker, String ipServidorCLima, int puertoServidorClima, String archivoCiudades) {
        this.authThread = authThread;
        this.ipBroker = ipBroker;
        this.puertoBroker = puertoBroker;
        this.ipServidorClima = ipServidorCLima;
        this.puertoServidorClima = puertoServidorClima;
        this.archivoCiudades = archivoCiudades;

        this.idPartida = UUID.randomUUID().toString();

        this.ciudades = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ciudades.add(new Ciudad());
        }

        this.partida = new Game();
    }

    private String leeSocket(Socket socketCliente) throws IOException {
        DataInputStream dis = new DataInputStream(socketCliente.getInputStream());
        return dis.readUTF();
    }

    private void escribeSocket(String mensaje, Socket socketCliente) throws IOException {
        DataOutputStream dos = new DataOutputStream(socketCliente.getOutputStream());
        dos.writeUTF(mensaje);
    }

    private boolean mandarPeticion(JSONObject res, Socket socketCliente) throws IOException {
        MessageParser parser = new MessageParser();

        StringBuilder sb = new StringBuilder();
        sb.append(MessageParser.STXChar);
        sb.append(res.toString());
        sb.append(MessageParser.ETXChar);
        sb.append(parser.getStringLRC(res.toString()));

        escribeSocket(sb.toString(), socketCliente);
        return true;
    }

    public void guardarCiudad(String nombreCiudad, JSONObject respuesta, int numeroCiudades) {
        ciudades.get(numeroCiudades).setNombre(nombreCiudad);
        ciudades.get(numeroCiudades).setTemperatura(Integer.parseInt(respuesta.get(nombreCiudad).toString()));
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
        Socket socketCliente = null;
        try {
            socketCliente = new Socket(ipServidorClima, puertoServidorClima);
        } catch (Exception e) {
            System.out.println("No se pudo conectar al servidor del clima. Usando ciudades por defecto.");
        }
        MessageParser parser = new MessageParser();
        ArrayList<String> nombreCiudades = obtenerCiudades();
        int temperaturasObtenidas = 0;
    
        try {
            if (socketCliente == null) {
                return;
            }

            String respuestaStr = "";

            escribeSocket(Character.toString(MessageParser.ENQChar), socketCliente);
            respuestaStr = leeSocket(socketCliente);

            if (!respuestaStr.equals(Character.toString(MessageParser.ACKChar))) {
                // Se ha recibido NAK al enviar ENQ. Parar peticiones y jugar con ciudades por defecto.
                System.out.println("Error en al iniciar la comunicación con el servidor del clima. Usando ciudades por defecto.");
                return;
            }

            for (String nombreCiudad : nombreCiudades) {
                if (temperaturasObtenidas >= 4) {
                    // Ya se tienen todas las temperaturas necesarias.
                    break;
                }
    
                JSONObject respuestaJSON = null;
                JSONObject peticion = new JSONObject();
                peticion.put("ciudad", nombreCiudad);
    
                mandarPeticion(peticion, socketCliente);
                respuestaStr = leeSocket(socketCliente);
    
                if (respuestaStr.equals(Character.toString(MessageParser.NAKChar))) {
                    // La ciudad no existe y no hay temperatura.
                    continue; 
                }
                else if (respuestaStr.equals(Character.toString(MessageParser.ACKChar))) {
                    try {
                        respuestaJSON = parser.parseMessage(leeSocket(socketCliente));
                        guardarCiudad(nombreCiudad, respuestaJSON, temperaturasObtenidas);
                        temperaturasObtenidas++;
                        escribeSocket(Character.toString(MessageParser.ACKChar), socketCliente);
                    } catch (MessageParserException e) {
                        // Hubo un error al recibir la temperatura de esta ciudad, pasar a la siguiente.
                        continue;
                    }
                }
            }
    
            escribeSocket(Character.toString(MessageParser.EOTChar), socketCliente);
            respuestaStr = leeSocket(socketCliente);
    
            if (respuestaStr.equals(Character.toString(MessageParser.EOTChar))) {
                socketCliente.close();
            }

            if (temperaturasObtenidas < 4) {
                System.out.println("No se pudieron obtener 4 ciudades. Algunas tendrán valores por defecto.");
            }
        } catch (IOException e) {
            System.out.println("Hubo una interrupción en la conexión con el servidor del clima. Algunas ciudades puede que tengan valores por defecto.");
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

        playerMovementsConsumer = new KafkaConsumer<>(p);
        playerMovementsConsumer.subscribe(Arrays.asList("PLAYERMOVEMENTS"));
    }

    private void inicializarProductorDeMapa() {
        Properties p = new Properties();
        p.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ipBroker + ":" + puertoBroker);
        p.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        mapProducer = new KafkaProducer<>(p);
    }

    private Jugador getPlayer(int token) {
        for (String alias : jugadores.keySet()) {
            if (jugadores.get(alias).getToken() == token) {
                return jugadores.get(alias);
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
                if (t - lastMovementsLogger.get(token) > 1000000) {
                    jugadoresDesconectados.add(j);
                }
            }
        }

        return jugadoresDesconectados;
    }

    private void removeDisconnectedPlayers(ArrayList<Jugador> jugadoresDesconectados, HashMap<Integer, Long> lastMovementsLogger) {
        for (Jugador j : jugadoresDesconectados) {
            System.out.println("El jugador " + j.getAlias() + " no ha hecho un movimiento por más de 10 segundos. Ha sido removido de la partida.");
            lastMovementsLogger.remove(j.getToken());
            jugadores.remove(j.getAlias());
            partida.removePlayerFromMap(j);
        }
    }

    private void gestionarPartida() {
        JSONParser parser = new JSONParser();
        HashMap<Integer, Long> lastMovementsLogger = new HashMap<>();

        mapProducer.send(new ProducerRecord<String,String>("MAP", partida.toJSONString()));

        long tiempoInicial = System.currentTimeMillis() / 1000;
        initLastMovementsLogger(lastMovementsLogger, tiempoInicial);
        // La partida termina en 2 minutos o cuando quede un jugador.
        while (jugadores.size() > 1 && ((System.currentTimeMillis() / 1000) - tiempoInicial) <= 120) {
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

            mapProducer.send(new ProducerRecord<String,String>("MAP", partida.toJSONString()));
            mapProducer.flush();
        }
    }

    @Override
    public void run() {
        

        try {
            obtenerTemperaturas();
        } catch (FileNotFoundException e1) {
            System.out.println("No se ha encontrado el archivo para leer las ciudades. Usando ciudades por defecto.");
        }
        partida.setCiudades(ciudades);

        try {
            authThread.join();

            jugadores = authThread.getJugadores();
            partida.setJugadores(jugadores);

            NPCAuthenticationHandler npcAuthHandler = new NPCAuthenticationHandler(idPartida, ipBroker, puertoBroker, jugadores, authThread.getTokenGenerator(), partida);
            npcAuthHandler.start();

            inicializarConsumidorDeMovimientosDeJugadores();
            inicializarProductorDeMapa();
            
            // Imprimir que la partida comenzará ahora.
            gestionarPartida();

            npcAuthHandler.stopThread();
            playerMovementsConsumer.close();
            mapProducer.close();
        } catch (InterruptedException e) {
            System.out.println("No se puede esperar al hilo de autenticación porque se ha interrumpido.");
        }
    }
}
