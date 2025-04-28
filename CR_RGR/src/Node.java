import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.Vector;

public class Node extends GraphObject implements Comparable{
    private final Coords pos;
    private final int number;
    private StackPane container;
    private final Vector<Arch> attachments = new Vector<>();
    private Color color;
    private int tin = -1;
    private int low = -1;

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

        this.color = Color.WHITE;
    }

    public void setHiddenColor(Color color) {
        this.color = color;
    }

    public void setColor(Color color) {
        figure.setFill(color);
        if(color == Color.BLACK) {
            Text txTemp = (Text) container.getChildren().getLast();
            txTemp.setStroke(Color.WHITE);
        }
        if(color == Color.GRAY || color == Color.WHITE) {
            Text txTemp = (Text) container.getChildren().getLast();
            txTemp.setStroke(Color.BLACK);
        }
    }

    public Color getColor() {
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

    public Text getText() {
        return (Text) container.getChildren().getLast();
    }

    public void setLow(int low) {
        this.low = low;
    }

    public void setTin(int tin) {
        this.tin = tin;
    }

    public int getLow() {
        return low;
    }

    public int getTin() {
        return tin;
    }
}
