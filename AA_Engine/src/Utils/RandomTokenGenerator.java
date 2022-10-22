package Utils;

import java.util.ArrayList;
import java.util.Random;

public class RandomTokenGenerator {
    private ArrayList<Integer> tokensUsadas;
    private final int numeroMaximo = Integer.MAX_VALUE;
    private final int numeroMinimo = 501;

    public RandomTokenGenerator() {
        tokensUsadas = new ArrayList<Integer>();
    }

    public ArrayList<Integer> getTokensUsadas() {
        return tokensUsadas;
    }

    public int generarToken() {
        Random rand = new Random();
        int numeroAleatorio;

        do {
            numeroAleatorio = rand.nextInt(numeroMinimo, numeroMaximo);
        } while (tokensUsadas.contains(numeroAleatorio));

        tokensUsadas.add(numeroAleatorio);

        return numeroAleatorio;
    }
}