package Game;

import org.json.simple.JSONArray;

public class Coordenada {
    private int fila;
    private int columna;

    public Coordenada(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    public int getFila() {
        return this.fila;
    }

    public void setFila(int fila) {
        this.fila = fila;
    }

    public int getColumna() {
        return this.columna;
    }

    public void setColumna(int columna) {
        this.columna = columna;
    }

    public JSONArray toJSONArray() {
        JSONArray arr = new JSONArray();
        arr.add(fila);
        arr.add(columna);
        return arr;
    }
}
