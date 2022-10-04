import java.io.*;
import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

class AA_Weather {
    private AA_Weather() {}

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Error missing argument.");
            System.out.println("Usage: java AA_Weather port");

            System.exit(-1);
        }

        JSONParser parser = new JSONParser();

        JSONObject bd = new JSONObject();
        try {
            bd = (JSONObject)parser.parse(new FileReader("./ciudades_temperaturas.json"));
        } catch (IOException e) {
            System.out.println("No se ha podido abrir el archivo con las ciudades y temperaturas.");
            System.out.println("Ejecucón terminada.");

            System.exit(-1);
        }
        catch (ParseException e) {
            System.out.println("El archivo con las ciudades y temperaturas no cumple el formato JSON.");
            System.out.println("Ejecucón terminada.");
            
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
    
                Thread hiloServidor = new AA_WeatherThread(socketCliente, bd);
                hiloServidor.start();
            }
        } catch (IOException e) {
            System.out.println("Socket couldn't be opened.");
        }
    }
}