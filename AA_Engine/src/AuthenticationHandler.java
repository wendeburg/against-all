import java.io.*;
import java.net.*;

import org.json.simple.JSONObject;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;

import Utils.RandomTokenGenerator;

public class AuthenticationHandler extends Thread {
    private final int puerto;
    private final int maxJugadores;
    private final MongoCollection coleccionUsuarios;
    public JSONObject[] jugadores;

    public static class ControlNumeroJugadores {
        public volatile int num = 0;
    }

    public AuthenticationHandler(int puerto, int maxJugadores, String ipDB, int puertoDB) {
        this.puerto = puerto;
        this.maxJugadores = maxJugadores;

        MongoClient cliente = new MongoClient(ipDB, puertoDB);
        coleccionUsuarios = cliente.getDatabase("against-all-db").getCollection("users");
        cliente.close();
    }

    @Override
    public void run() {
        ServerSocket socketServidor;
        ControlNumeroJugadores numJugadores = new ControlNumeroJugadores();
        
        try {
            socketServidor = new ServerSocket(puerto);
            System.out.println("Servidor de autenticación escuchando en el puerto: " + puerto);
            RandomTokenGenerator tokenGenerator = new RandomTokenGenerator();

            long tiempoInicial = System.currentTimeMillis() / 1000;
            // Se debería utilizar el semásforo para antes de leer num_jugadores.
            // Se empieza la partida cuando se llena de jugadores o han pasado 120 segundos.
            while((numJugadores.num <= maxJugadores) && ((System.currentTimeMillis() / 1000) - tiempoInicial) <= 120) {
                Socket socketCliente = socketServidor.accept();
    
                Thread hiloServidor = new AuthenticationHandlerThread(socketCliente, coleccionUsuarios, tokenGenerator, numJugadores);
                hiloServidor.start();
            }
        } catch (IOException e) {
            System.out.println("El socket no se puedo abrir.");
        }
    }
}
