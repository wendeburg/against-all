import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.json.simple.JSONObject;

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
    private ArrayList<Ciudad> ciudades;
    private HashMap<String, Jugador> jugadores;
    private Game partida;

    public GameHandler(AuthenticationHandler authThread, String ipBroker, int puertoBroker, String ipServidorCLima, int puertoServidorClima, String archivoCiudades) {
        this.authThread = authThread;
        this.ipBroker = ipBroker;
        this.puertoBroker = puertoBroker;
        this.ipServidorClima = ipServidorCLima;
        this.puertoServidorClima = puertoServidorClima;
        this.archivoCiudades = archivoCiudades;
    
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

    private void gestionarPartida() {
        
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

            gestionarPartida();
        } catch (InterruptedException e) {
            System.out.println("No se puede esperar al hilo de autenticación porque se ha interrumpido.");
        }
    }
}
