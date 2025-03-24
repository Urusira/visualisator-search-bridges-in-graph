import javafx.scene.shape.Circle;

public class Node extends GraphObject {
    private Coords pos;

    public Coords getPos() {
        return pos;
    }

    public void setPos(Coords pos) {
        this.pos = pos;
    }

    public Node(Coords pos, Circle circle) {
        this.pos = pos;
        figure = circle;
    }
}
