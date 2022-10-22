import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleInputWaiterThread extends Thread {
    AuthenticationHandler.ConsoleInput consoleInput;

    public ConsoleInputWaiterThread(AuthenticationHandler.ConsoleInput consoleInput) {
        this.consoleInput = consoleInput;
    }

    @Override
    public void run() {
        String input = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (!input.equals("q")) {
            try {
                input = br.readLine();
            } catch (IOException e) {
                System.out.println("Error al leer de consola.");
            }
        }

        System.out.println("He muerto, consoleinputthread");
        consoleInput.input = "q";
    }
}
