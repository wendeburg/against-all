package Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Game {
    private ArrayList<ArrayList<IColocable>> mapa;
    private final int tamanoMapa = 20;
    private final int porcentajeMinas = 20;
    private final int porcentajeAlimentos = 19;
    private HashMap<String, Jugador> jugadores;
    private ArrayList<Ciudad> ciudades;

    public Game() {
        initMap();
    }

    private void initMap() {
        mapa = new ArrayList<>();

        for (int i = 0; i < tamanoMapa; i++) {
            ArrayList<IColocable> arr = new ArrayList<>();

            for (int j = 0; j < 20; j++) {
                arr.add(new EspacioVacio());
            }

            mapa.add(arr);
        }

        generateMinesAndFood();
    }

    private void generateMinesAndFood() {
        Random rand = new Random();

        for (ArrayList<IColocable> fila : mapa) {
            for (int j = 0; j < fila.size(); j++) {
                int alimento = rand.nextInt(100);
                int mina = rand.nextInt(100);

                if (mina < porcentajeMinas) {
                    fila.set(j, new Mina());
                }
                else if (alimento < porcentajeAlimentos) {
                    fila.set(j, new Alimento());
                }
            }
        }
    }

    public void setJugadores(HashMap<String, Jugador> jugadores) {
        this.jugadores = jugadores;
        addPlayersToMap();
    }

    public void setCiudades(ArrayList<Ciudad> ciudades) {
        this.ciudades = ciudades;
    }

    private void addPlayersToMap() {
        Random rand = new Random();

        for (String key : jugadores.keySet()) {
            boolean playerSet = false;

            while (!playerSet) {
                int randomRow = rand.nextInt(tamanoMapa);
                int randomColumn = rand.nextInt(tamanoMapa);

                if (mapa.get(randomRow).get(randomColumn) instanceof EspacioVacio) {
                    playerSet = true;
                    mapa.get(randomRow).set(randomColumn, jugadores.get(key));
                }
            }
        }
    }

    private Coordenada obtenerNuevaPosicion(String direccion, Coordenada posAnterior) {
        Coordenada nuevaPos = null;

        switch (direccion) {
            case "N":
                nuevaPos = new Coordenada((posAnterior.getFila() + 1) % 19, posAnterior.getColumna());
                break;
            case "S":
                nuevaPos = new Coordenada((posAnterior.getFila() - 1) % 19, posAnterior.getColumna());
                break;
            case "E":
                nuevaPos = new Coordenada(posAnterior.getFila(), (posAnterior.getColumna() - 1) % 19);
                break;
            case "W":
                nuevaPos = new Coordenada(posAnterior.getFila(), (posAnterior.getColumna() + 1) % 19);
                break;
            case "NE":
                nuevaPos = new Coordenada((posAnterior.getFila() + 1) % 19, (posAnterior.getColumna() - 1) % 19);
                break;
            case "NW":
                nuevaPos = new Coordenada((posAnterior.getFila() + 1) % 19, (posAnterior.getColumna() + 1) % 19);
                break;
            case "SE":
                nuevaPos = new Coordenada((posAnterior.getFila() - 1) % 19, (posAnterior.getColumna() - 1) % 19);
                break;
            case "SW":
                nuevaPos = new Coordenada((posAnterior.getFila() - 1) %19, (posAnterior.getColumna() + 1) % 19);
                break;
        }

        return nuevaPos;
    }

    public Ciudad obtenerCiudadDeCoordenada(Coordenada c) {
        if (c.getFila() < 10 && c.getColumna() < 10) {
            return ciudades.get(0);
        }
        else if (c.getFila() >= 10 && c.getColumna() < 10) {
            return ciudades.get(2);
        }
        else if (c.getFila() < 10 && c.getColumna() >= 10) {
            return ciudades.get(1);
        }
        else {
            return ciudades.get(3);
        }
    }

    private void computarEfectosDeTemperatura(Jugador j, Coordenada posActual, Coordenada posNueva) {
        Ciudad ciudadAntigua = obtenerCiudadDeCoordenada(posActual);
        Ciudad ciudadNueva = obtenerCiudadDeCoordenada(posNueva);

        if (!ciudadAntigua.equals(ciudadNueva)) {
            if (ciudadNueva.getTemperatura() <= 10) {
                j.modificarNivel(j.getEfectoFrio());
            }
            else if (ciudadNueva.getTemperatura() >= 25) {
                j.modificarNivel(j.getEfectoCalor());
            }
        }
    }

    public void movePlayer(String direccion, Jugador jugador) {
        Coordenada posAnterior = jugador.getPosicion();
        Coordenada nuevaPos = obtenerNuevaPosicion(direccion, posAnterior);
        IColocable colocableEnNuevaPos = mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna());
        boolean jugadorMovido = false;
        boolean jugadorMuerto = false;

        if (colocableEnNuevaPos instanceof Mina) {
            jugador.setPosicion(new Coordenada(-1, -1));
            mapa.get(nuevaPos.getFila()).set(nuevaPos.getColumna(), new EspacioVacio());
            jugadorMuerto = true;
        }
        else if (colocableEnNuevaPos instanceof Alimento) {
            jugador.addOneLevel();
            mapa.get(nuevaPos.getFila()).set(nuevaPos.getColumna(), jugador);
            jugador.setPosicion(nuevaPos);
            computarEfectosDeTemperatura(jugador, posAnterior, nuevaPos);
            jugadorMovido = true;
        }
        else if (colocableEnNuevaPos instanceof Jugador) {
            Jugador adversario = (Jugador) colocableEnNuevaPos;
            // Se asume que se debe primero aplicar los efectos del clima sobre el jugador que se mueve antes de la lucha.
            computarEfectosDeTemperatura(jugador, posAnterior, nuevaPos);

            if (adversario.getNivel() < jugador.getNivel()) {
                adversario.setPosicion(new Coordenada(-1, -1));
                jugador.setPosicion(nuevaPos);
                mapa.get(nuevaPos.getFila()).set(nuevaPos.getColumna(), jugador);
                jugadorMovido = true;
            }
            else {
                // Que pasa en caso de empate?? En este caso gana el jugador que estaba en la posicion de destino.
                jugador.setPosicion(new Coordenada(-1, -1));
                jugadorMuerto = true;
            }
        }
        else {
            mapa.get(nuevaPos.getFila()).set(nuevaPos.getColumna(), jugador);
            jugador.setPosicion(nuevaPos);
            computarEfectosDeTemperatura(jugador, posAnterior, nuevaPos);
            jugadorMovido = true;
        }

        if (jugadorMovido || jugadorMuerto) {
            mapa.get(posAnterior.getFila()).set(posAnterior.getColumna(), new EspacioVacio());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (ArrayList<IColocable> fila : mapa) {
            for (int j = 0; j < fila.size(); j++) {
                sb.append(fila.get(j).getNumberRepresentation());
                sb.append(" ");
            }

            sb.append("\n");
        }
        
        return sb.toString();
    }
}
