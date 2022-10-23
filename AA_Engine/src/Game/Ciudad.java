package Game;

public class Ciudad {
    private static int nextID = 1;
    private String nombre;
    private float temperatura;

    public Ciudad() {
        this.nombre = "Ciudad " + nextID;
        this.temperatura = 20;

        nextID++;
    }

    public Ciudad(String nombre, float temperatura) {
        this.nombre = nombre;
        this.temperatura = temperatura;
    
        nextID++;
    }

    public String getNombre() {
        return nombre;
    }

    public float getTemperatura() {
        return temperatura;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setTemperatura(float temperatura) {
        this.temperatura = temperatura;
    }

    @Override
    public String toString() {
        return nombre + ": " + temperatura;
    }


    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        
        if (!(o instanceof Ciudad)) {
            return false;
        }

        Ciudad otraCiudad = (Ciudad) o;
        return this.nombre.equals(otraCiudad.nombre) && temperatura == otraCiudad.temperatura;
    }
}
