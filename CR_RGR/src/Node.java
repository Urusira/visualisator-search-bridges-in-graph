import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.Vector;

public class Node extends GraphObject implements Comparable{
    private final Coords pos;
    private final int number;
    private StackPane container;
    private final Vector<Arch> attachments = new Vector<>();
    private nodeColors color;
    private int amoVisit;

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

        this.color = nodeColors.WHITE;

        amoVisit = 0;
    }

    public void setColor(nodeColors color) {
        this.color = color;
    }

    public void recolor() {
        switch (amoVisit) {
            case 0 -> {
                figure.setFill(Color.GRAY);
                break;
            }
            case 1 -> {
                figure.setFill(Color.BLACK);
                Text txt = (Text) container.getChildren().getLast();
                txt.setStroke(Color.WHITE);
                break;
            }
            default -> {
                return;
            }
        }
        amoVisit++;
    }

    public nodeColors getColor() {
        return color;
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
