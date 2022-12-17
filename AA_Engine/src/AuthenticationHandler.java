import java.io.File;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.HashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.bson.Document;

import com.mongodb.client.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import Game.Jugador;
import Utils.RandomTokenGenerator;

public class AuthenticationHandler extends Thread {
    private final int puerto;
    private final int maxJugadores;
    private final MongoClient cliente;
    private final MongoCollection<Document> coleccionUsuarios;
    private final HashMap<String, Jugador> jugadores;
    RandomTokenGenerator tokenGenerator;

    public AuthenticationHandler(int puerto, int maxJugadores, String ipDB, int puertoDB, RandomTokenGenerator tokenGenerator) {
        this.puerto = puerto;
        this.maxJugadores = maxJugadores;
        this.jugadores = new HashMap<>();
        this.tokenGenerator = tokenGenerator;

        cliente = MongoClients.create("mongodb://" + ipDB + ":" + puertoDB);
        MongoDatabase db = cliente.getDatabase("against-all-db");
        coleccionUsuarios = db.getCollection("users");
    }

    public RandomTokenGenerator getTokenGenerator() {
        return tokenGenerator;
    }

    public static class ConsoleInput {
        public volatile String input = "";
    }

    public HashMap<String, Jugador> getJugadores() {
        return jugadores;
    }

    @Override
    public void run() {
        SSLServerSocket socketServidor = null;
        
        try {
            // Contraseña del KeyStore.
            char[] ksPassword = Files.readString(Path.of("./secrets/aa_engine_keystore_creds")).toCharArray();
            
            // Cargar KeyStore.
            KeyStore keyStore = KeyStore.getInstance(new File("./secrets/engine.keystore.jks"), ksPassword);
            
            // Incializar TrustStore a partir de Keystore.
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
            keyManagerFactory.init(keyStore, ksPassword);

            // Crear contexto SSL.
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            
            // Crear fábrica de servidores SSLSocket.
            SSLServerSocketFactory sslServerSocketFactory = ctx.getServerSocketFactory();

            socketServidor = (SSLServerSocket) sslServerSocketFactory.createServerSocket(3001);
            System.out.println("Servidor de autenticación escuchando en el puerto: " + puerto);
            System.out.println("Para cerrar el servidor de autenticación presiona \"q\"");

            ClientWaiterThread waiter = new ClientWaiterThread(socketServidor, coleccionUsuarios, jugadores, tokenGenerator, maxJugadores);

            ConsoleInput ci = new ConsoleInput();
            ConsoleInputWaiterThread ciwt = new ConsoleInputWaiterThread(ci);

            long tiempoInicial = System.currentTimeMillis() / 1000;
            waiter.start();
            ciwt.start();
            // Se empieza la partida cuando se llena de jugadores o han pasado 120 segundos.
            while(jugadores.size() < maxJugadores && ((System.currentTimeMillis() / 1000) - tiempoInicial) <= 120 && !ci.input.equals("q")) {
                // Esperar a que alguna condiciómn se cumpla para terminar con el hilo.
            }

            return;
        } catch (Exception e) {
            System.out.println("El socket no se puedo abrir.");
            System.out.println(e);
        }
        finally {
            System.out.println("Servidor de autenticación cerrado.");
            cliente.close();
            try {
                socketServidor.close();
            } catch (Exception e) {
                System.out.println("Error al intentar cerrar el socket del servidor de autentiación.");
            }
        }
    }
}
