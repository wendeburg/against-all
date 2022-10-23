package Game;

public class Jugador {
    private int nivel;
    private int token;
    private String alias;
    private int efectoFrio;
    private int efectoCalor;


    public Jugador(int nivel, int token, String alias, int efectoFrio, int efectoCalor) {
        this.nivel = nivel;
        this.token = token;
        this.alias = alias;
        this.efectoFrio = efectoFrio;
        this.efectoCalor = efectoCalor;
    }

    public int getNivel() {
        return this.nivel;
    }

    public void addOneLevel() {
        this.nivel++;;
    }

    public String getAlias() {
        return this.alias;
    }

    public int getToken() {
        return this.token;
    }

    public int getEfectoFrio() {
        return this.efectoFrio;
    }

    public int getEfectoCalor() {
        return this.efectoCalor;
    }
}
