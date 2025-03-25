import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Vector;

public class Node extends GraphObject implements Comparable{
    private final Coords pos;
    private final int number;
    private final Vector<Arch> attachments = new Vector<>();

    public Coords getPos() {
        return pos;
    }

    public Node(Coords pos, Circle circle, int num) {
        circle.setStrokeWidth(3);
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.BLACK);

        this.pos = pos;
        figure = circle;
        number = num;
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

    @Override
    public int compareTo(Object o) {
        Node node = (Node) o;
        return Integer.compare(this.number, node.number);
    }
}
