import java.io.*;
import java.net.*;
import java.util.HashMap;

import org.bson.Document;
import org.json.simple.JSONObject;

import com.mongodb.client.MongoCollection;

import Utils.*;

public class AuthenticationHandlerThread extends Thread {
    private final Socket socketCliente;
    private final String dirIPCliente;
    private final MongoCollection<Document> usuarios;
    private final RandomTokenGenerator tokenGenerator;
    private final HashMap<String, Integer> jugadores;

    public AuthenticationHandlerThread(Socket socketCliente, MongoCollection<Document> usuarios, HashMap<String, Integer> jugadores, RandomTokenGenerator tokenGenerator) {
        this.socketCliente = socketCliente;
        this.usuarios = usuarios;
        this.jugadores = jugadores;
        this.tokenGenerator = tokenGenerator;
        
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

    private boolean mandarRespuesta(JSONObject res) {
        MessageParser parser = new MessageParser();

        StringBuilder sb = new StringBuilder();
        sb.append(MessageParser.STXChar);
        sb.append(res.toString());
        sb.append(MessageParser.ETXChar);
        sb.append(parser.getStringLRC(res.toString()));

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
        Document userToAuthenticate = new Document("alias", peticion.get("alias").toString());
        Document user = usuarios.find(userToAuthenticate).first();

        if (user != null && user.get("password").toString().equals(peticion.get("password").toString())) {
            if (!jugadores.containsKey(user.get("alias"))) {
                JSONObject respuesta = new JSONObject();
                int tokenGenerada = tokenGenerator.generarToken();

                jugadores.put(user.get("alias").toString(), tokenGenerada);

                respuesta.put("token", tokenGenerada);
                
                mandarRespuesta(respuesta);
    
                return true;
            }
        }

        try {
            escribeSocket(Character.toString(MessageParser.NAKChar));
            return false;
        } catch (IOException e) {
            System.out.println("Error al enviar NAK al cliente con ip: " + dirIPCliente);
            return false;
        }
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
            } catch (Exception e) {
                try {
                    if (!(e instanceof EOFException)) {
                        escribeSocket(Character.toString(MessageParser.NAKChar));
                    }
                    else {
                        break;
                    }
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
