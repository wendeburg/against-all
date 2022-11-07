package Game;

public class Jugador implements IColocable {
    private int nivel;
    private int token;
    private String alias;
    private int efectoFrio;
    private int efectoCalor;
    private Coordenada posicion;
    private boolean isNPC;

    public Jugador(int nivel, int token, String alias, int efectoFrio, int efectoCalor) {
        this.nivel = nivel;
        this.token = token;
        this.alias = alias;
        this.efectoFrio = efectoFrio;
        this.efectoCalor = efectoCalor;
        this.isNPC = false;
    }

    public void setAsNPC() {
        this.isNPC = true;
    }

    public boolean getIsNPC() {
        return this.isNPC;
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

    public Coordenada getPosicion() {
        return posicion;
    }

    public void setPosicion(Coordenada posicion) {
        this.posicion = posicion;
    }

    public void modificarNivel(int valor) {
        this.nivel += valor;

        if (this.nivel < 0) {
            this.nivel = 0;
        }
    }

    @Override
    public int getNumberRepresentation() {
        return this.token;
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        Jugador otroJugador = (Jugador) o;
        // field comparison
        return this.token == otroJugador.getToken();
    }
}
