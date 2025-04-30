import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.Vector;

public class Node extends GraphObject implements Comparable{
    private final Coords pos;
    private int number;
    private final StackPane container;
    private final Text textNum;
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

        textNum = new Text(Integer.toString(num));
        circleContainer.getChildren().add(textNum);

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
            textNum.setStroke(Color.WHITE);
        }
        if(color == Color.GRAY || color == Color.WHITE) {
            textNum.setStroke(Color.BLACK);
        }
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

    public void setNumber(int v) {
        number = v;
        textNum.setText(String.valueOf(v));
    }

    public StackPane getContainer() {
        return container;
    }

    @Override
    public int compareTo(Object o) {
        Node node = (Node) o;
        return Integer.compare(getNumber(), node.getNumber());
    }

    public void delAttach(Arch arch) {
        attachments.remove(arch);
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
