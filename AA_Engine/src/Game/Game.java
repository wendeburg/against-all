package Game;

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
    private ArrayList<Ciudad> ciudades;

    public Game() {
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

        for (int i = 0; i < colocablesEnNuevaPos.getColocables().size() && !jugadorMovido && !jugadorMuerto; i++) {
            IColocable c = colocablesEnNuevaPos.getColocables().get(i);

            if (c instanceof Mina) {
                if (jugador.getIsNPC()) {
                    continue;
                }

                jugadores.remove(jugador.getAlias());

                if (colocablesEnNuevaPos.getColocables().size() == 1) {
                    mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).removeElementAt(i);
                    mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).addColocalble(new EspacioVacio());
                }
                else {
                    mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).removeElementAt(i);
                }

                jugadorMuerto = true;
            }
            else if (c instanceof Alimento) {
                if (jugador.getIsNPC()) {
                    continue;
                }

                jugador.addOneLevel();

                if (colocablesEnNuevaPos.getColocables().size() == 1) {
                    mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).removeElementAt(i);
                    mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).addColocalble(jugador);
                    jugador.setPosicion(nuevaPos);
                    computarEfectosDeTemperatura(jugador, posAnterior, nuevaPos);
                    jugadorMovido = true;
                }
                else {
                    mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).removeElementAt(i);
                }
            }
            else if (c instanceof Jugador) {
                Jugador adversario = (Jugador) c;
                // Se asume que se debe primero aplicar los efectos del clima sobre el jugador que se mueve antes de la lucha.
                
                if (!jugador.getIsNPC()) {
                    computarEfectosDeTemperatura(jugador, posAnterior, nuevaPos);
                }
    
                if (adversario.getNivel() < jugador.getNivel()) {
                    adversario.setPosicion(new Coordenada(-1, -1));
                    jugadores.remove(adversario.getAlias());

                    if (colocablesEnNuevaPos.getColocables().size() > 1) {
                        continue;
                    }

                    jugador.setPosicion(nuevaPos);
                    mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).removeElementAt(i);
                    mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).addColocalble(jugador);
                    jugadorMovido = true;
                }
                else if (adversario.getNivel() > jugador.getNivel()) {
                    jugadores.remove(jugador.getAlias());
                    jugador.setPosicion(new Coordenada(-1, -1));
                    
                    jugadorMuerto = true;
                }
            }
            else {
                if (i != (colocablesEnNuevaPos.getColocables().size()-1)) {
                    mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).removeElementAt(i);
                }

                removeEmptySpace(colocablesEnNuevaPos);
                mapa.get(nuevaPos.getFila()).get(nuevaPos.getColumna()).addColocalble(jugador);
                jugador.setPosicion(nuevaPos);

                if (!jugador.getIsNPC()) {
                    computarEfectosDeTemperatura(jugador, posAnterior, nuevaPos);
                }                
                
                jugadorMovido = true;
            }
    
            if (jugadorMovido || jugadorMuerto) {
                removePlayerFromPosition(jugador, posAnterior);
                mapa.get(posAnterior.getFila()).get(posAnterior.getColumna()).addColocalble(new EspacioVacio());
            }   
        }
    }

    private void removeEmptySpace(Celda celda) {
        for (int i = 0; i < celda.getColocables().size(); i++) {
            IColocable c = celda.getColocables().get(i);
            
            if (c instanceof EspacioVacio) {
                celda.removeElementAt(i);
                break;
            }
        }
    }

    private void removePlayerFromPosition(Jugador j, Coordenada coord) {
        ArrayList<IColocable> colocables = mapa.get(coord.getFila()).get(coord.getColumna()).getColocables();

        for (int i = 0; i < colocables.size(); i++) {
            IColocable c = colocables.get(i);

            if (c instanceof Jugador) {
                if (j.equals(c)) {
                    mapa.get(coord.getFila()).get(coord.getColumna()).removeElementAt(i);
                }
            }
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
        JSONObject citiesJSON = getCiudadesAsJSONObject();
        
        JSONObject obj = new JSONObject();
        obj.put("mapa", mapaJSON);
        obj.put("jugadores", jugadoresJSON);
        obj.put("ciudades", citiesJSON);
        
        return obj.toString();
    }
}
