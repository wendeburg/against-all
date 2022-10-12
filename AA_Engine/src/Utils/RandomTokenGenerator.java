package Utils;

import java.util.ArrayList;
import java.util.Random;

public class RandomTokenGenerator {
    private ArrayList<Integer> tokensUsadas;
    private final int numeroMaximo = Integer.MAX_VALUE;

    public RandomTokenGenerator() {
        tokensUsadas = new ArrayList<Integer>();
    }

    public int generarToken() {
        Random rand = new Random();
        int numeroAleatorio;

        do {
            numeroAleatorio = rand.nextInt(numeroMaximo);
        } while (tokensUsadas.contains(numeroAleatorio));

        tokensUsadas.add(numeroAleatorio);

        return numeroAleatorio;
    }
}