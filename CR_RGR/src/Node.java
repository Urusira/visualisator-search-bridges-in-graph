import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Vector;

public class Node extends GraphObject implements Comparable{
    private final Coords pos;
    private int number;
    private final StackPane container;
    private final Text text;
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

        text = new Text(num+"\ntin= \nlow= ");
        text.setTextAlignment(TextAlignment.CENTER);
        circleContainer.getChildren().add(text);



        this.pos = pos;
        number = num;

        container = circleContainer;

        this.color = Color.WHITE;
    }

//    public void setHiddenColor(Color color) {
//        this.color = color;
//    }

    public void setColor(Color color) {
        if(color == this.color) return;
        figure.setFill(color);
        this.color = color;
        if(color == Color.BLACK) {
            text.setFill(Color.WHITE);
        }
        if(color == Color.GRAY || color == Color.WHITE) {
            text.setFill(Color.BLACK);
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
        updateText();
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

    public Color getColor() {
        return color;
    }

    public void updateText() {
        if(tin == -1 || low == -1)
            text.setText(number + "\ntin= \nlow= ");
        else
            text.setText(number +"\ntin="+ tin +"\nlow="+ low);
    }

    @Override
    public String toString() {
        return String.valueOf(number);
    }
}
