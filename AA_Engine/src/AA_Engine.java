import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
        String archivoGuardadoEstadoPartida;
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
            archivoGuardadoEstadoPartida = args[9];

            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String chosenOption = "";

            while (!chosenOption.equals("3")) {
                System.out.println("\n>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<");
                System.out.println(">>>>>> AGAINST ALL ENGINE <<<<<<");
                System.out.println(">>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<\n");
    
                System.out.println("Opciones:");
                System.out.println("1. Iniciar partida.");
                System.out.println("2. Recuperar última partida.");
                System.out.println("3. Salir.");

                try {
                    chosenOption = consoleReader.readLine();
                }
                catch (IOException e) {
                    System.out.println("Ha habido un error al leer de consola.");
                }

                if (chosenOption.equals("1")) {
                    AuthenticationHandler authThread = new AuthenticationHandler(puerto, maxJugadores, ipDB, puertoDB);
                    authThread.start();
                    
                    GameHandler gameThread = new GameHandler(authThread, ipBroker, puertoBroker, ipServidorClima, puertoServidorClima, archivoCiudades, archivoGuardadoEstadoPartida);
                    gameThread.start();
        
                    try {
                        gameThread.join();
                    } catch (InterruptedException e) {
                        System.out.println("No se puede esperar al hilo del juego porque se ha interrumpido.");
                    }
                }
                else if (chosenOption.equals("2")) {
                    Path filePath = Path.of("./estado_ultima_partida.json");
                    String content;
                    try {
                        content = Files.readString(filePath);

                        JSONParser parser = new JSONParser();

                        JSONObject estadoUltimaPartida = (JSONObject) parser.parse(content);

                        int resultadoVerificacion = verifySavedGameState(estadoUltimaPartida);

                        if (resultadoVerificacion == 0) {
                            // Mandar a GameHandler.
                        }
                        else {
                            String razon = "";
                            switch (resultadoVerificacion) {
                                case 1: razon = "El formato del archivo no es correcto.";
                                        break;
                                case 2: razon = "La partida ya ha finalizado.";
                                        break;
                            }

                            System.out.println("No se peude recuperar la última partida del archivo " + archivoGuardadoEstadoPartida + ". Razón: " + razon);
                        }
                    } catch (IOException e) {
                        System.out.println("No se peude recuperar la última partida del archivo " + archivoGuardadoEstadoPartida + ". Razón: No se puede leer el archivo.");
                    } catch (ParseException e) {
                        System.out.println("No se peude recuperar la última partida del archivo " + archivoGuardadoEstadoPartida + ". Razón: El formato del archivo no es correcto.");
                    }
                }
                else if (!chosenOption.equals("3")) {
                    System.out.println("Opción no conocida. Inténtalo de nuevo.");
                }
            }
            
            System.exit(0);

        }
        catch (NumberFormatException e) {
            System.out.println("Error en tipo de argumentos.");
            System.out.println("Uso: java -jar AA_Engine.jar puerto max_jugadores ip_sv_clima puerto_sv_clima ip_broker puerto_broker ip_bd puerto_bd");

            System.exit(-1);
        }
    }

    private static Integer verifySavedGameState(JSONObject estadoUltimaPartida) {
        if (estadoUltimaPartida.size() > 6 || estadoUltimaPartida.size() < 6) {
            return 1;
        }

        boolean gameFinished = (boolean) estadoUltimaPartida.get("gamefinished");
        if (gameFinished) {
            return 2;
        }

        return 0;
    }
}