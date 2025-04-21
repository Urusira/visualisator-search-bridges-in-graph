import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

public abstract class GraphObject {
    private boolean hasVisited = false;
    private boolean highlight = false;
    protected Shape figure;

    public boolean isVisited() {
        return hasVisited;
    }

    public void visit() {
        hasVisited = true;
        figure.setFill(Color.LIGHTGRAY);
        figure.setStroke(Color.GRAY);
    }

    public void turnOffHighlight() {
        highlight = false;
        figure.setStroke(Color.BLACK);
    }

    public void turnOnHighlight() {
        highlight = true;
        figure.setStroke(Color.LIME);
    }

    public void select() {
        figure.setStroke(Color.LIGHTGRAY);
    }
    public void deSelect() {
        figure.setStroke(Color.BLACK);
    }

    public Shape getFigure() {
        return figure;
    }
}
