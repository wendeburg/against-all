import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import javax.management.RuntimeErrorException;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import Utils.RandomTokenGenerator;

public class ClientWaiterThread extends Thread {
    private final ServerSocket socketServidor;
    private final MongoCollection<Document> coleccionUsuarios;
    private final HashMap<String, Integer> jugadores;
    private final RandomTokenGenerator tokenGenerator;

    public ClientWaiterThread(ServerSocket socketServidor, MongoCollection<Document> coleccionUsuarios, HashMap<String, Integer> jugadores, RandomTokenGenerator tokenGenerator) {
        this.socketServidor = socketServidor;
        this.coleccionUsuarios = coleccionUsuarios;
        this.jugadores = jugadores;
        this.tokenGenerator = tokenGenerator;
    }

    public void run() {
        Socket socketCliente;
        try {
            socketCliente = socketServidor.accept();

            Thread hiloServidor = new AuthenticationHandlerThread(socketCliente, coleccionUsuarios, jugadores, tokenGenerator);
            hiloServidor.start();
        } catch (IOException e) {
            throw new RuntimeErrorException(null);
        }
    }
}
