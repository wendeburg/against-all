package Game;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Game {
    private ArrayList<ArrayList<Celda>> mapa;
    private final int tamanoMapa = 20;
    private final int porcentajeMinas = 10;
    private final int porcentajeAlimentos = 15;
    private HashMap<String, Jugador> jugadores;
    private HashMap<String, Jugador> NPCs;
    private ArrayList<Ciudad> ciudades;

    public Game(HashMap<String, Jugador> NPCs) {
        this.NPCs = NPCs;
        initMap();
    }

    private void initMap() {
        mapa = new ArrayList<>();

        for (int i = 0; i < tamanoMapa; i++) {
            ArrayList<Celda> arr = new ArrayList<>();

            for (int j = 0; j < 20; j++) {
                arr.add(new Celda(new EspacioVacio()));
            }

            mapa.add(arr);
        }

        generateMinesAndFood();
    }

    private void generateMinesAndFood() {
        Random rand = new Random();

        for (ArrayList<Celda> fila : mapa) {
            for (int j = 0; j < fila.size(); j++) {
                int alimento = rand.nextInt(100);
                int mina = rand.nextInt(100);

                if (mina < porcentajeMinas) {
                    fila.get(j).removeElementAt(0);
                    fila.get(j).addColocalble(new Mina());
                }
                else if (alimento < porcentajeAlimentos) {
                    fila.get(j).removeElementAt(0);
                    fila.get(j).addColocalble(new Alimento());
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

                if (mapa.get(randomRow).get(randomColumn).getElementAt(0) instanceof EspacioVacio) {
                    playerSet = true;
                    jugadores.get(key).setPosicion(new Coordenada(randomRow, randomColumn));
                    mapa.get(randomRow).get(randomColumn).removeElementAt(0);
                    mapa.get(randomRow).get(randomColumn).addColocalble(jugadores.get(key));
                }
            }
        }
    }

    private int obtenerColumnaOFila(int posAnterior, int cambio) {
        int nuevaPos = posAnterior + cambio;

        if (nuevaPos > 19) {
            return 0;
        }
        else if (nuevaPos < 0) {
            return 19;
        }
        
        return nuevaPos;
    }

    private Coordenada obtenerNuevaPosicion(String direccion, Coordenada posAnterior) {
        Coordenada nuevaPos = null;

        switch (direccion) {
            case "S":
                nuevaPos = new Coordenada(obtenerColumnaOFila(posAnterior.getFila(), 1), posAnterior.getColumna());
                break;
            case "N":
                nuevaPos = new Coordenada(obtenerColumnaOFila(posAnterior.getFila(), -1), posAnterior.getColumna());
                break;
            case "W":
                nuevaPos = new Coordenada(posAnterior.getFila(), obtenerColumnaOFila(posAnterior.getColumna(), -1));
                break;
            case "E":
                nuevaPos = new Coordenada(posAnterior.getFila(), obtenerColumnaOFila(posAnterior.getColumna(), 1));
                break;
            case "SW":
                nuevaPos = new Coordenada(obtenerColumnaOFila(posAnterior.getFila(), 1), obtenerColumnaOFila(posAnterior.getColumna(), -1));
                break;
            case "SE":
                nuevaPos = new Coordenada(obtenerColumnaOFila(posAnterior.getFila(), 1), obtenerColumnaOFila(posAnterior.getColumna(), 1));
                break;
            case "NW":
                nuevaPos = new Coordenada(obtenerColumnaOFila(posAnterior.getFila(), -1), obtenerColumnaOFila(posAnterior.getColumna(), -1));
                break;
            case "NE":
                nuevaPos = new Coordenada(obtenerColumnaOFila(posAnterior.getFila(), -1), obtenerColumnaOFila(posAnterior.getColumna(), 1));
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

    public void addPlayerToMap(Jugador jugador) {
        Random rand = new Random();
        boolean playerSet = false;

        while (!playerSet) {
            int randomRow = rand.nextInt(tamanoMapa);
            int randomColumn = rand.nextInt(tamanoMapa);

            if (mapa.get(randomRow).get(randomColumn).getElementAt(0) instanceof EspacioVacio) {
                playerSet = true;
                jugador.setPosicion(new Coordenada(randomRow, randomColumn));
                mapa.get(randomRow).get(randomColumn).removeElementAt(0);
                mapa.get(randomRow).get(randomColumn).addColocalble(jugador);
            }
        }
    }

    public void movePlayer(String direccion, Jugador jugador) {
        Coordenada posAnterior = jugador.getPosicion();
        Coordenada nuevaPos = obtenerNuevaPosicion(direccion, posAnterior);
        Celda colocablesEnNuevaPos = mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna());
        boolean jugadorMovido = false;
        boolean jugadorMuerto = false;
        ArrayList<IColocable> colocablesAEliminar = new ArrayList<>();
        ArrayList<IColocable> nuevosColocalbesEnCelda = new ArrayList<>();


        for (int i = 0; i < colocablesEnNuevaPos.getColocables().size() && !jugadorMovido && !jugadorMuerto; i++) {
            IColocable c = colocablesEnNuevaPos.getColocables().get(i);

            if (c instanceof Mina) {
                if (jugador.getIsNPC()) {
                    continue;
                }

                jugadores.remove(jugador.getAlias());

                //mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).removeElementAt(i);
                //mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).addColocalble(new EspacioVacio());

                colocablesAEliminar.add(c);
                jugadorMuerto = true;
            }
            else if (c instanceof Alimento) {
                if (jugador.getIsNPC()) {
                    continue;
                }

                jugador.addOneLevel();

                if (colocablesEnNuevaPos.getColocables().size() == 1) {
                    nuevosColocalbesEnCelda.add(jugador);
                    jugador.setPosicion(nuevaPos);
                    computarEfectosDeTemperatura(jugador, posAnterior, nuevaPos);
                    jugadorMovido = true;
                }

                colocablesAEliminar.add(c);
            }
            else if (c instanceof Jugador) {
                Jugador adversario = (Jugador) c;
                // Se asume que se debe primero aplicar los efectos del clima sobre el jugador que se mueve antes de la lucha.
                
                if (!jugador.getIsNPC()) {
                    computarEfectosDeTemperatura(jugador, posAnterior, nuevaPos);
                }
    
                if (adversario.getNivel() < jugador.getNivel()) {
                    adversario.setPosicion(new Coordenada(-1, -1));

                    if (adversario.getIsNPC()) {
                        NPCs.remove(adversario.getAlias());
                    }
                    else {
                        jugadores.remove(adversario.getAlias());
                    }

                    jugador.setPosicion(nuevaPos);
                    colocablesAEliminar.add(c);
                    nuevosColocalbesEnCelda.add(jugador);
                    jugadorMovido = true;
                }
                else if (adversario.getNivel() > jugador.getNivel()) {
                    jugador.setPosicion(new Coordenada(-1, -1));

                    if (jugador.getIsNPC()) {
                        NPCs.remove(jugador.getAlias());
                    }
                    else {
                        jugadores.remove(jugador.getAlias());
                    }
                    
                    jugadorMuerto = true;
                }
            }
            else { // Es un espacio vacío.
                jugador.setPosicion(nuevaPos);
                colocablesAEliminar.add(c);
                nuevosColocalbesEnCelda.add(jugador);

                if (!jugador.getIsNPC()) {
                    computarEfectosDeTemperatura(jugador, posAnterior, nuevaPos);
                }                
                
                jugadorMovido = true;
            }
        }

        if (jugadorMovido || jugadorMuerto) {
            removePlayerFromPosition(jugador, posAnterior);
        }

        updateCelda(colocablesEnNuevaPos, colocablesAEliminar, nuevosColocalbesEnCelda);
    }

    private void updateCelda(Celda celda, ArrayList<IColocable> colocablesAEliminar, ArrayList<IColocable> nuevosColocables) {
        ArrayList<IColocable> colocablesEnCelda = celda.getColocables();

        for (IColocable c : colocablesAEliminar) {
            colocablesEnCelda.remove(c);
        }

        for (IColocable c : nuevosColocables) {
            colocablesEnCelda.add(c);
        }
    }

    private void removePlayerFromPosition(Jugador j, Coordenada coord) {
        ArrayList<IColocable> colocables = mapa.get(coord.getFila()).get(coord.getColumna()).getColocables();

        for (int i = 0; i < colocables.size(); i++) {
            IColocable c = colocables.get(i);

            if (c instanceof Jugador) {
                if (j.equals(c)) {
                    colocables.remove(c);
                    break;
                }
            }
        }

        if (colocables.size() == 0) {
            colocables.add(new EspacioVacio());
        }
    }

    public void removePlayerFromMap(Jugador j) {
        removePlayerFromPosition(j, j.getPosicion());
    }

    private JSONObject getCiudadesAsJSONObject() {
        JSONObject obj = new JSONObject();
        
        for (Ciudad c : ciudades) {
            obj.put(c.getNombre(), c.getTemperatura());
        }

        return obj;
    }

    private JSONObject getPlayersAsJSONObject() {
        JSONObject obj = new JSONObject();

        for (String alias : jugadores.keySet()) {
            Jugador j = jugadores.get(alias);
            JSONObject jugador = new JSONObject();

            jugador.put("nivel", j.getNivel());
            jugador.put("posicion", j.getPosicion().toJSONArray());

            obj.put(alias, jugador);
        }

        return obj;
    }

    private JSONObject getNPCsAsJSONObject() {
        JSONObject obj = new JSONObject();

        for (String alias : NPCs.keySet()) {
            Jugador j = NPCs.get(alias);
            JSONObject jugador = new JSONObject();

            jugador.put("nivel", j.getNivel());
            jugador.put("posicion", j.getPosicion().toJSONArray());

            obj.put(alias, jugador);
        }

        return obj;
    }

    private JSONArray getMapAsJSONArray() {
        JSONArray mapaJSON = new JSONArray();

        for (ArrayList<Celda> fila : mapa) {
            JSONArray filaJSON = new JSONArray();

            for (Celda c : fila) {
                filaJSON.add(c.toJSONArray());
            }

            mapaJSON.add(filaJSON);
        }

        return mapaJSON;
    }

    public String toJSONString() {
        JSONArray mapaJSON = getMapAsJSONArray();
        JSONObject jugadoresJSON = getPlayersAsJSONObject();
        JSONObject npcsJSON = getNPCsAsJSONObject();
        JSONObject citiesJSON = getCiudadesAsJSONObject();
        
        JSONObject obj = new JSONObject();
        obj.put("mapa", mapaJSON);
        obj.put("jugadores", jugadoresJSON);
        obj.put("npcs", npcsJSON);
        obj.put("ciudades", citiesJSON);
        
        return obj.toString();
    }
}
