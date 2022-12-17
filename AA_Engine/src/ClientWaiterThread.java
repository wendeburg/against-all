import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import javax.net.ssl.SSLServerSocket;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import Game.Jugador;
import Utils.RandomTokenGenerator;

public class ClientWaiterThread extends Thread {
    private final SSLServerSocket socketServidor;
    private final MongoCollection<Document> coleccionUsuarios;
    private final HashMap<String, Jugador> jugadores;
    private final RandomTokenGenerator tokenGenerator;
    private final int maxJugadores;

    public ClientWaiterThread(SSLServerSocket socketServidor, MongoCollection<Document> coleccionUsuarios, HashMap<String, Jugador> jugadores, RandomTokenGenerator tokenGenerator, int maxJugadores) {
        this.socketServidor = socketServidor;
        this.coleccionUsuarios = coleccionUsuarios;
        this.jugadores = jugadores;
        this.tokenGenerator = tokenGenerator;
        this.maxJugadores = maxJugadores;
    }

    public void run() {
        Socket socketCliente;
        try {
            while (jugadores.size() < maxJugadores) {
                socketCliente = socketServidor.accept();

                Thread hiloServidor = new AuthenticationHandlerThread(socketCliente, coleccionUsuarios, jugadores, tokenGenerator, maxJugadores);
                hiloServidor.start();
            }
        } catch (IOException e) {
            // No hacer nada.
        }
    }
}
