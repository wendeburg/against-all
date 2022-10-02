package AA_Weather;

import java.io.*;
import java.net.*;

class AA_Weather {
    private AA_Weather() {}

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Error missing argument.");
            System.out.println("Usage: java AA_Weather port");

            System.exit(-1);
        }

        int port = Integer.parseInt(args[0]);

        ServerSocket socketServidor;

        try {
            socketServidor = new ServerSocket(port);
            System.out.println("Escuchando en el puerto: " + port);

            while(true) {
                Socket socketCliente = socketServidor.accept();
                InetSocketAddress clientAddress = (InetSocketAddress) socketCliente.getRemoteSocketAddress();
                String clientIPAddress = clientAddress.getAddress().getHostAddress();
    
                System.out.println("Sirviendo a cliente con ip: " + clientIPAddress);
    
                Thread hiloServidor = new AA_Weather_Thread(socketCliente);
                hiloServidor.start();
            }
        } catch (IOException e) {
            System.out.println("Socket couldn't be opened.");
        }
    }
}