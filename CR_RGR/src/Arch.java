import javafx.scene.shape.Line;

public class Arch extends GraphObject {
    private final Node[] linkedNodes = new Node[2];

    public Node[] getTransitNodes() {
        return linkedNodes;
    }

    public Arch(Node firstNode, Node secondNode, Line line) {
        linkedNodes[0] = firstNode;
        linkedNodes[1] = secondNode;
        figure = line;
    }
}
