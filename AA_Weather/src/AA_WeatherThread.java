import java.io.*;
import java.net.*;

import org.json.simple.*;
import Utils.MessageParser;
import Utils.MessageParserException;

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

    private JSONObject getCiudad(String nombre) {        
        JSONObject ciudadJSON = new JSONObject();

        ciudadJSON.put(nombre, bd.get(nombre));

        return ciudadJSON;
    }

    private boolean mandarRespuesta(JSONObject ciudades) throws IOException {
        MessageParser parser = new MessageParser();

        StringBuilder sb = new StringBuilder();
        sb.append(MessageParser.STXChar);
        sb.append(ciudades.toString());
        sb.append(MessageParser.ETXChar);
        sb.append(parser.getStringLRC(ciudades.toString()));

        escribeSocket(Character.toString(MessageParser.ACKChar));
        escribeSocket(sb.toString());
        return true;
    }

    private boolean gestionarPeticion(JSONObject peticion) throws IOException {
        Object nombreCiudadObj = peticion.get("ciudad");

        if (nombreCiudadObj != null) {
            String nombreCiudad = nombreCiudadObj.toString();

            try {
                JSONObject ciudadRespuesta= getCiudad(nombreCiudad);

                if (ciudadRespuesta.get(nombreCiudad) != null) {
                    return mandarRespuesta(ciudadRespuesta); 
                }
                else {
                    escribeSocket(Character.toString(MessageParser.NAKChar));
                    return false;
                }
               
            }
            catch (NumberFormatException e) {
                try {
                    escribeSocket(Character.toString(MessageParser.NAKChar));
                } catch (IOException e1) {
                    System.out.println("Error al enviar NAK al cliente con ip: " + dirIPCliente);
                }
            }
        }

        try {
            escribeSocket(Character.toString(MessageParser.NAKChar));
        } catch (IOException e1) {
            System.out.println("Error al enviar NAK al cliente con ip: " + dirIPCliente);
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
                }
                else if (!(respuestaEnviada && mensaje.equals(Character.toString(MessageParser.ACKChar)))) {
                    MessageParser parser = new MessageParser();

                    JSONObject peticion = parser.parseMessage(mensaje);

                    respuestaEnviada = gestionarPeticion(peticion);
                }
                else if (respuestaEnviada && mensaje.equals(Character.toString(MessageParser.ACKChar))) {
                    respuestaEnviada = false;
                }
            } catch (MessageParserException e) {
                try {
                    escribeSocket(Character.toString(MessageParser.NAKChar));
                } catch (IOException e1) {
                    System.out.println("Error al enviar NAK al cliente con ip: " + dirIPCliente);
                    cont = false;
                }
            }
            catch (IOException e) {
                System.out.println("Hubo una interrupción en la conexión con el cliente con ip: " + dirIPCliente);
                cont = false;
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
