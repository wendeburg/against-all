import java.io.*;
import java.net.*;
import java.util.HashMap;

import org.bson.Document;

import com.mongodb.client.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import Utils.RandomTokenGenerator;

public class AuthenticationHandler extends Thread {
    private final int puerto;
    private final int maxJugadores;
    private final MongoClient cliente;
    private final MongoCollection<Document> coleccionUsuarios;
    private final HashMap<String, Integer> jugadores;

    public AuthenticationHandler(int puerto, int maxJugadores, String ipDB, int puertoDB, HashMap<String, Integer> jugadores) {
        this.puerto = puerto;
        this.maxJugadores = maxJugadores;
        this.jugadores = jugadores;

        cliente = MongoClients.create("mongodb://" + ipDB + ":" + puertoDB);
        MongoDatabase db = cliente.getDatabase("against-all-db");
        coleccionUsuarios = db.getCollection("users");
    }

    @Override
    public void run() {
        ServerSocket socketServidor;
        RandomTokenGenerator tokenGenerator = new RandomTokenGenerator();
        
        try {
            socketServidor = new ServerSocket(puerto);
            System.out.println("Servidor de autenticación escuchando en el puerto: " + puerto);
            System.out.println("Para cerrar el servidor de autenticación presiona \"q\"");

            ClientWaiterThread waiter = new ClientWaiterThread(socketServidor, coleccionUsuarios, jugadores, tokenGenerator);

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            long tiempoInicial = System.currentTimeMillis() / 1000;
            waiter.start();
            // Se empieza la partida cuando se llena de jugadores o han pasado 120 segundos.
            while((tokenGenerator.getTokensUsadas().size() <= maxJugadores && ((System.currentTimeMillis() / 1000) - tiempoInicial) <= 120) ) {
                String input = br.readLine();

                if (input.equals("q")) {
                    break;
                }
            }

            return;
        } catch (Exception e) {
            System.out.println("El socket no se puedo abrir.");
        }
        finally {
            System.out.println("Servidor de autenticación cerrado");
            cliente.close();
        }
    }
}
