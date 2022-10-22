import java.io.*;
import java.net.*;
import java.util.HashMap;

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

    public AuthenticationHandler(int puerto, int maxJugadores, String ipDB, int puertoDB, HashMap<String, Jugador> jugadores) {
        this.puerto = puerto;
        this.maxJugadores = maxJugadores;
        this.jugadores = jugadores;

        cliente = MongoClients.create("mongodb://" + ipDB + ":" + puertoDB);
        MongoDatabase db = cliente.getDatabase("against-all-db");
        coleccionUsuarios = db.getCollection("users");
    }

    public static class ConsoleInput {
        public volatile String input = "";
    }

    @Override
    public void run() {
        ServerSocket socketServidor = null;
        RandomTokenGenerator tokenGenerator = new RandomTokenGenerator();
        
        try {
            socketServidor = new ServerSocket(puerto);
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
                // Wait for something to finish thread.
            }

            return;
        } catch (Exception e) {
            System.out.println("El socket no se puedo abrir.");
        }
        finally {
            System.out.println("Servidor de autenticación cerrado");
            cliente.close();
            try {
                socketServidor.close();
            } catch (Exception e) {
                System.out.println("Error al intentar cerrar el socket del servidor de autentiación");
            }
        }
    }
}
