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
    }

    public void turnHighlight() {
        if(highlight) {
            highlight = false;
            figure.setStroke(Color.BLACK);
        }
        else {
            highlight = true;
            figure.setStroke(Color.LIME);
        }
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
