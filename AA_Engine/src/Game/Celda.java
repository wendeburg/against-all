package Game;

import java.util.ArrayList;

import org.json.simple.JSONArray;

public class Celda {
    private ArrayList<IColocable> celda;

    public Celda() {
        this.celda = new ArrayList<>();
    }

    public Celda(IColocable c) {
        this.celda = new ArrayList<>();
        this.celda.add(c);
    }

    public void addColocalble(IColocable c) {
        this.celda.add(c);
    }

    public ArrayList<IColocable> getColocables() {
        return this.celda;
    }

    public IColocable getElementAt(int i) {
        return this.celda.get(i);
    }

    public void removeElementAt(int i) {
        this.celda.remove(i);
    }

    public JSONArray toJSONArray() {
        JSONArray jsonArr = new JSONArray();
        
        for (IColocable c : celda) {
            jsonArr.add(c.getNumberRepresentation());
        }

        return jsonArr;
    }
}
