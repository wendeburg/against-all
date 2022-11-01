import java.util.UUID;

class AA_Engine {
    private AA_Engine() {}

    public static void main(String[] args) {
        if (args.length < 9) {
            System.out.println("Error faltan argumentos.");
            System.out.println("Uso: java -jar AA_Engine.jar puerto max_jugadores ip_sv_clima puerto_sv_clima ip_broker puerto_broker ip_bd puerto_bd archivo_con_ciudades");

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
        String archivoCiudades;
        try {
            puerto = Integer.parseInt(args[0]);
            maxJugadores = Integer.parseInt(args[1]);
            ipServidorClima = args[2];
            puertoServidorClima = Integer.parseInt(args[3]);
            ipBroker = args[4];
            puertoBroker = Integer.parseInt(args[5]);
            ipDB = args[6];
            puertoDB = Integer.parseInt(args[7]);
            archivoCiudades = args[8];

            String idPartida = UUID.randomUUID().toString();

            AuthenticationHandler authThread = new AuthenticationHandler(puerto, maxJugadores, ipDB, puertoDB);
            authThread.start();
            
            GameHandler gameThread = new GameHandler(authThread, ipBroker, puertoBroker, ipServidorClima, puertoServidorClima, archivoCiudades);
            gameThread.start();

            try {
                gameThread.join();
            } catch (InterruptedException e) {
                System.out.println("No se puede esperar al hilo d autenticación porque se ha interrumpido.");
            }
            
            System.exit(0);

        }
        catch (NumberFormatException e) {
            System.out.println("Error en tipo de argumentos.");
            System.out.println("Uso: java -jar AA_Engine.jar puerto max_jugadores ip_sv_clima puerto_sv_clima ip_broker puerto_broker ip_bd puerto_bd");

            System.exit(-1);
        }
    }
}