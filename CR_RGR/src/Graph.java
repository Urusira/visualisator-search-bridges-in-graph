import javafx.scene.layout.Pane;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

public class Graph implements Serializable {
    private static final long serialVersionUID = 1L;

    private final TreeMap<Node, LinkedHashSet<Node>> graph;

    public Graph() {
        graph = new TreeMap<>();
    }

    public TreeMap<Node, LinkedHashSet<Node>> getGraph() {
        return graph;
    }

    public boolean addNode(Node node) {
        graph.put(node, new LinkedHashSet<>());
        return graph.containsKey(node);
    }

    public int len() {
        return graph.size();
    }

    public boolean addAttach(Node node1, Node node2) {
        if(node1 == node2) {
            System.out.println("WARNING\t\tNode cannot be attached with self. Operation is canceled.");
            return false;
        }

        if(graph.get(node1).contains(node2) || graph.get(node2).contains(node1)) {
            System.out.println("WARNING\t\tHas attached! Operation is canceled.");
            return false;
        }

        graph.get(node1).add(node2);
        graph.get(node2).add(node1);

        return true;
    }

    public String getStrValues(Node node) {
        StringBuilder str = new StringBuilder();
        for (var it : graph.get(node)) {
            str.append(it.getNumber());
            if (it != graph.get(node).getLast()) {
                str.append(", ");
            }
        }
        return str.toString();
    }

    public Node findWithNum(int num) {
        Set<Node> nodes = graph.keySet();

        for(Node node : nodes) {
            if(node.getNumber() == num) {
                return node;
            }
        }
        return null;
    }

    public void deleteArch(Node firstNode, Node secondNode, Arch arch, Pane drawSpace) {
        drawSpace.getChildren().remove(arch.getFigure());
        graph.get(firstNode).remove(secondNode);
        graph.get(secondNode).remove(firstNode);
        firstNode.delAttach(arch);
        secondNode.delAttach(arch);
    }

    public Arch findArch(Node firstNode, Node secondNode) {
        for(Arch arch : firstNode.getAttachments()){
            if(arch.getTransitNodes()[0] == secondNode || arch.getTransitNodes()[1] == secondNode) {
                return arch;
            }
        }
        return null;
    }

    public void deleteNode(Node node, Pane drawSpace) {
        drawSpace.getChildren().removeAll(node.getFigure(), node.getContainer());
        Vector<Arch> attachments = node.getAttachments();
        while(!attachments.isEmpty()) {
            Arch arch = attachments.firstElement();
            Node[] transitNodes = arch.getTransitNodes();
            deleteArch(transitNodes[0], transitNodes[1], arch, drawSpace);
        }
        graph.tailMap(node, false).forEach((node1, nodes) -> node1.setNumber(node1.getNumber()-1));
        graph.remove(node);
    }

    public Vector<Node> getNodes() {
        return new Vector<>(graph.keySet());
    }

    public Vector<Node> getAttaches(Node node) {
        return new Vector<>(graph.get(node).stream().toList());
    }

    public boolean isNear(Coords checkableCoords, double minDist) {
        for(var node : getNodes()) {
            if(Coords.minus(node.getPos(), checkableCoords) < minDist) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        for(Node node : getNodes()) {
            string.append(node.getNumber())
                    .append(" attached")
                    .append(Arrays.toString(getNodes().toArray()))
                    .append(", cords")
                    .append(node.getPos().toString())
                    .append("\n");
        }
        return string.toString();
    }
}
