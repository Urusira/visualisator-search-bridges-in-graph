import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Vector;

public class Node extends GraphObject implements Comparable{
    private final Coords pos;
    private final int number;
    private StackPane container;
    private final Vector<Arch> attachments = new Vector<>();

    public Coords getPos() {
        return pos;
    }

    public Node(Coords pos, StackPane circleContainer, int num) {
        figure = circleContainer.getShape();
        figure.setStrokeWidth(2);
        figure.setFill(Color.WHITE);
        figure.setStroke(Color.BLACK);

        this.pos = pos;
        number = num;

        container = circleContainer;
    }

    public void addAttachment(Arch arch) {
        attachments.add(arch);
    }

    public Vector<Arch> getAttachments() {
        return attachments;
    }

    public int getNumber() {
        return number;
    }

    public StackPane getContainer() {
        return container;
    }

    @Override
    public int compareTo(Object o) {
        Node node = (Node) o;
        return Integer.compare(this.number, node.number);
    }

    public void delAttach(Arch arch) {
        attachments.remove(arch);
    }
}
