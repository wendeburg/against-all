import java.io.*;
import java.net.*;

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
    private final RandomTokenGenerator tokenGenerator;

    public AuthenticationHandler(int puerto, int maxJugadores, String ipDB, int puertoDB, RandomTokenGenerator tokenGenerator) {
        this.puerto = puerto;
        this.maxJugadores = maxJugadores;
        this.tokenGenerator = tokenGenerator;

        cliente = MongoClients.create("mongodb://" + ipDB + ":" + puertoDB);
        MongoDatabase db = cliente.getDatabase("against-all-db");
        coleccionUsuarios = db.getCollection("users");
    }

    @Override
    public void run() {
        ServerSocket socketServidor;
        
        try {
            socketServidor = new ServerSocket(puerto);
            System.out.println("Servidor de autenticación escuchando en el puerto: " + puerto);

            long tiempoInicial = System.currentTimeMillis() / 1000;
            // Se empieza la partida cuando se llena de jugadores o han pasado 120 segundos.
            while((tokenGenerator.getTokensUsadas().size() <= maxJugadores && ((System.currentTimeMillis() / 1000) - tiempoInicial) <= 120) ) {
                Socket socketCliente = socketServidor.accept();
    
                Thread hiloServidor = new AuthenticationHandlerThread(socketCliente, coleccionUsuarios, tokenGenerator);
                hiloServidor.start();
            }
        } catch (IOException e) {
            System.out.println("El socket no se puedo abrir.");
        }
        finally {
            System.out.println("Servidor de autenticación cerrado");
            cliente.close();
        }
    }
}
