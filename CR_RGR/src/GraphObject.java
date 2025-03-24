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
        figure.getStyleClass().add("-fx-background-color: LIGHTGRAY;");
    }

    public boolean isHighlight() {
        return highlight;
    }

    public void turnHighlight() {
        if(highlight) {
            highlight = false;
            figure.setStyle("-fx-fill: BLACK;");
        }
        else {
            highlight = true;
            figure.setStyle("-fx-fill: LIME;");
        }
    }

    public Shape getFigure() {
        return figure;
    }
}
