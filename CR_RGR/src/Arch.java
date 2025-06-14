import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.util.Objects;

public class Arch extends GraphObject {
    private final Node[] linkedNodes = new Node[2];

    public Node[] getTransitNodes() {
        return linkedNodes;
    }

    public Arch(Node firstNode, Node secondNode, Line line) {
        line.setStrokeWidth(3);
        line.setStroke(Color.BLACK);
        line.toBack();

        linkedNodes[0] = firstNode;
        linkedNodes[1] = secondNode;
        figure = line;
    }

    @Override
    public void turnOnHighlight() {
        figure.setStroke(Color.VIOLET);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;

        Arch another = (Arch) obj;

        return(
                (Objects.equals(linkedNodes[0], another.getTransitNodes()[0]) &&
                Objects.equals(linkedNodes[1], another.getTransitNodes()[1])) ||
                Objects.equals(linkedNodes[0], another.getTransitNodes()[1]) &&
                Objects.equals(linkedNodes[1], another.getTransitNodes()[0])
        );
    }

    @Override
    public String toString() {
        return linkedNodes[0]+":"+linkedNodes[1];
    }
}
