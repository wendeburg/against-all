package AA_Weather;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import org.json.simple.*;
import Utils.MessageParser;
import Utils.MessageParserException;

public class AA_Weather_Thread extends Thread {
    private final Socket socketCliente;
    private final String dirIPCliente;
    private final JSONObject bd;

    protected AA_Weather_Thread(Socket socketCliente, JSONObject bd) {
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

    private void mandarRespuesta(JSONObject ciudades) {
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

            while (cont && numIntentos < 6) {
                mensaje = leeSocket();
                
                if (mensaje.equals(Character.toString(MessageParser.ACKChar))) {
                    cont = false;
                }
                else {
                    numIntentos++;
            
                    escribeSocket(sb.toString());
                }
                
                try {
                    sleep(2000); // Sleep for 2s before sending again.
                }
                catch (InterruptedException e) {
                    System.out.println("No se pudo pausar hilo sirviendo al cliente con ip: " + dirIPCliente);
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error al utilizar socket con cliente con ip: " + dirIPCliente);
        }
    }

    private void gestionarPeticion(JSONObject peticion) {
        String numCiudadesStr = (String)peticion.get("ciudades");

        if (numCiudadesStr != null) {
            try {
                int numeroCiudades = Integer.parseInt(numCiudadesStr);

                JSONObject ciudadesRespuesta = getCiudades(numeroCiudades);

                mandarRespuesta(ciudadesRespuesta);                
            }
            catch (NumberFormatException e) {
                try {
                    escribeSocket(Character.toString(MessageParser.NAKChar));
                } catch (IOException e1) {
                    System.out.println("Error al enviar NAK al cliente con ip: " + dirIPCliente);
                }
            }
        }
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
