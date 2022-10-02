package AA_Weather;

import java.io.*;
import java.net.*;
import org.json.simple.*;
import Utils.MessageParser;
import Utils.MessageParserException;

public class AA_Weather_Thread extends Thread {
    private final Socket socketCliente;
    private final String dirIPCliente;

    protected AA_Weather_Thread(Socket socketCliente) {
        this.socketCliente = socketCliente;
        
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

    private void gestionarPeticion(JSONObject peticion) {

    }

    @Override
    public void run() {
        String mensaje;
        boolean cont = true;
    
        while (cont) {
            try {
                mensaje = leeSocket();

                if (mensaje.equals(Character.toString(MessageParser.ENQChar))) {
                    escribeSocket(Character.toString(MessageParser.ACKChar));
                }
                else if (mensaje.equals(Character.toString(MessageParser.EOTChar))) {
                    cont = false;
                }
                else {
                    MessageParser parser = new MessageParser();

                    try {
                        JSONObject peticion = parser.parseMessage(mensaje);

                        gestionarPeticion(peticion);
                    } catch (MessageParserException e) {
                        try {
                            escribeSocket(Character.toString(MessageParser.NAKChar));
                        } catch (IOException e1) {
                            System.out.println("Error al enviar NAK al cliente con ip: " + dirIPCliente);
                        }
                    }
                }
            } catch (IOException e) {
                try {
                    escribeSocket(Character.toString(MessageParser.NAKChar));
                } catch (IOException e1) {
                    System.out.println("Error al enviar NAK al cliente con ip: " + dirIPCliente);
                }
            }
            finally {
                try {
                    socketCliente.close();
                    System.out.println("Cerrada conexión con cliente con ip: " + dirIPCliente);
                } catch (IOException e) {
                    System.out.println("No se pudo cerrar el socket con el cliente con ip: " + dirIPCliente);
                }
            }
        }
    }
}
