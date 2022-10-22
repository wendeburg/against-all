import java.util.HashMap;

import Game.Jugador;

class AA_Engine {
    private AA_Engine() {}

    public static void main(String[] args) {
        if (args.length < 8) {
            System.out.println("Error faltan argumentos.");
            System.out.println("Uso: java -jar AA_Engine.jar puerto max_jugadores ip_sv_clima puerto_sv_clima ip_broker puerto_broker ip_bd puerto_bd");

            System.exit(-1);
        }

        int puerto;
        int maxJugadores;
        String ipServidorClima;
        int puertoServidorClima;
        String ipBroker;
        int puertoBroker;
        String ipDB;
        int puertoDB;
        try {
            puerto = Integer.parseInt(args[0]);
            maxJugadores = Integer.parseInt(args[1]);
            ipServidorClima = args[2];
            puertoServidorClima = Integer.parseInt(args[3]);
            ipBroker = args[4];
            puertoBroker = Integer.parseInt(args[5]);
            ipDB = args[6];
            puertoDB = Integer.parseInt(args[7]);

            HashMap<String, Jugador> jugadoresAutenticados = new HashMap<>();
            AuthenticationHandler authThread = new AuthenticationHandler(puerto, maxJugadores, ipDB, puertoDB, jugadoresAutenticados);
            authThread.start();
            
            //GameHandler gameThread = new GameHandler(authThread, ipBroker, puertoBroker, ipServidorClima, puertoServidorClima);
            //gameThread.start();

            authThread.join();
            //gameThread.setJugadores(jugadoresAutenticados);
        }
        catch (NumberFormatException e) {
            System.out.println("Error en tipo de argumentos.");
            System.out.println("Uso: java -jar AA_Engine.jar puerto max_jugadores ip_sv_clima puerto_sv_clima ip_broker puerto_broker ip_bd puerto_bd");

            System.exit(-1);
        } catch (InterruptedException e) {
            System.out.println("No se puede esperar al hilo d autenticación porque se ha interrumpido.");
        }
    }
}