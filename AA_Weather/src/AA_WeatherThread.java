import java.io.*;
import java.net.*;
import java.util.Random;

import org.json.simple.*;
import Utils.MessageParser;

public class AA_WeatherThread extends Thread {
    private final Socket socketCliente;
    private final String dirIPCliente;
    private final JSONObject bd;

    protected AA_WeatherThread(Socket socketCliente, JSONObject bd) {
        this.socketCliente = socketCliente;
        this.bd = bd;
        
        InetSocketAddress direccionCliente = (InetSocketAddress) socketCliente.getRemoteSocketAddress();
        this.dirIPCliente = direccionCliente.getAddress().getHostAddress();
    }

    private String leeSocket() throws IOException {
        DataInputStream dis = new DataInputStream(this.socketCliente.getInputStream());
        return dis.readUTF();
    }

    private void escribeSocket(String mensaje) throws IOException {
        DataOutputStream dos = new DataOutputStream(this.socketCliente.getOutputStream());
        dos.writeUTF(mensaje);
    }

    private JSONObject getCiudades(int num) {        
        Random rand = new Random();
        JSONObject ciudades = new JSONObject();

        Object[] claves = bd.keySet().toArray();

        for (int i = 0; i < num; i++) {
            int indice = rand.nextInt(bd.size());

            ciudades.put(claves[indice], bd.get(claves[indice]));
        }

        return ciudades;
    }

    private boolean mandarRespuesta(JSONObject ciudades) {
        int numIntentos = 0;
        boolean cont = true;
        String mensaje;
        MessageParser parser = new MessageParser();

        StringBuilder sb = new StringBuilder();
        sb.append(MessageParser.STXChar);
        sb.append(ciudades.toString());
        sb.append(MessageParser.ETXChar);
        sb.append(parser.getStringLRC(ciudades.toString()));

        try {
            escribeSocket(sb.toString());
            return true;
        }
        catch (IOException e) {
            System.out.println("Error al utilizar socket con cliente con ip: " + dirIPCliente);
            return false;
        }
    }

    private boolean gestionarPeticion(JSONObject peticion) {
        String numCiudadesStr = peticion.get("ciudades").toString();

        if (numCiudadesStr != null) {
            try {
                int numeroCiudades = Integer.parseInt(numCiudadesStr);

                JSONObject ciudadesRespuesta = getCiudades(numeroCiudades);

                return mandarRespuesta(ciudadesRespuesta);                
            }
            catch (NumberFormatException e) {
                try {
                    escribeSocket(Character.toString(MessageParser.NAKChar));
                } catch (IOException e1) {
                    System.out.println("Error al enviar NAK al cliente con ip: " + dirIPCliente);
                }
            }
        }

        return false;
    }

    @Override
    public void run() {
        System.out.println("Sirviendo a cliente con ip: " + dirIPCliente);

        boolean cont = true;
        boolean respuestaEnviada = false;
    
        while (cont) {
            try {
                String mensaje = leeSocket();

                if (mensaje.equals(Character.toString(MessageParser.ENQChar))) {
                    escribeSocket(Character.toString(MessageParser.ACKChar));
                }
                else if (mensaje.equals(Character.toString(MessageParser.EOTChar))) {
                    cont = false;

                    escribeSocket(Character.toString(MessageParser.EOTChar));

                    socketCliente.close();
                    System.out.println("Cerrada conexión con cliente con ip: " + dirIPCliente);
                }
                else if (!(respuestaEnviada && mensaje.equals(Character.toString(MessageParser.ACKChar)))) {
                    MessageParser parser = new MessageParser();

                    JSONObject peticion = parser.parseMessage(mensaje);

                    respuestaEnviada = gestionarPeticion(peticion);
                }
            } catch (Exception e) {
                try {
                    escribeSocket(Character.toString(MessageParser.NAKChar));
                } catch (IOException e1) {
                    System.out.println("Error al enviar NAK al cliente con ip: " + dirIPCliente);
                    cont = false;
                }
            }
        }
    
        try {
            socketCliente.close();
            System.out.println("Cerrada conexión con cliente con ip: " + dirIPCliente);
        } catch (IOException e) {
            System.out.println("No se pudo cerrar el socket con el cliente con ip: " + dirIPCliente);
        }
    }
}
