package Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Mapa {
    private ArrayList<ArrayList<Integer>> mapa;
    private final int tamanoMapa = 20;
    private final int porcentajeMinas = 20;
    private final int porcentajeAlimentos = 19;

    public Mapa() {
        mapa = new ArrayList<>();
        
        for (int i = 0; i < tamanoMapa; i++) {
            mapa.add(new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        }

        generateMinesAndFood();
    }

    private void generateMinesAndFood() {
        Random rand = new Random();

        for (ArrayList<Integer> fila : mapa) {
            for (int j = 0; j < fila.size(); j++) {
                int alimento = rand.nextInt(100);
                int mina = rand.nextInt(100);

                if (mina < porcentajeMinas) {
                    fila.set(j, 2);
                }
                else if (alimento < porcentajeAlimentos) {
                    fila.set(j, 1);
                }
            }
        }
    }

    public void addPlayers(HashMap<String, Jugador> jugadores) {
        Random rand = new Random();

        for (String key : jugadores.keySet()) {
            boolean playerSet = false;
            int playerToken = jugadores.get(key).getToken();

            while (!playerSet) {
                int randomRow = rand.nextInt(tamanoMapa);
                int randomColumn = rand.nextInt(tamanoMapa);

                if (mapa.get(randomRow).get(randomColumn) == 0) {
                    playerSet = true;
                    mapa.get(randomRow).set(randomColumn, playerToken);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (ArrayList<Integer> fila : mapa) {
            for (int j = 0; j < fila.size(); j++) {
                sb.append(fila.get(j));
                sb.append(" ");
            }

            sb.append("\n");
        }
        
        return sb.toString();
    }
}
