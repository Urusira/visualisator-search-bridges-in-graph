import javafx.scene.layout.Pane;

import java.util.*;

public class Graph {
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
        String result = "";
        for (var it : graph.get(node)) {
            result = result.concat(Integer.toString(it.getNumber()));
            if (it != graph.get(node).getLast()) {
                result += ", ";
            }
        }
        return result;
    }

    public Set<Node> getKeys() {
        return graph.keySet();
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
        delArchInTable(firstNode, secondNode);
        firstNode.delAttach(arch);
        secondNode.delAttach(arch);
    }
    public void delArchInTable(Node firstNode, Node secondNode) {
        graph.get(firstNode).remove(secondNode);
        graph.get(secondNode).remove(firstNode);
    }

    public void deleteNode(Node node, Pane drawSpace) {
        drawSpace.getChildren().removeAll(node.getFigure(), node.getContainer());
        Vector<Arch> attachments = node.getAttachments();
        while(!attachments.isEmpty()) {
            Arch arch = attachments.firstElement();
            Node[] transitNodes = arch.getTransitNodes();
            deleteArch(transitNodes[0], transitNodes[1], arch, drawSpace);
        }
        for(var it : graph.entrySet()) {
            it.getValue().remove(node);
        }
        graph.remove(node);
    }

    public Vector<Node> getNodes() {
        return new Vector<>(graph.keySet());
    }

    public boolean isNear(Coords checkableCoords, double minDist) {
        for(var node : graph.keySet()) {
            if(Coords.minus(node.getPos(), checkableCoords) < minDist) {
                return false;
            }
        }
        return true;
    }
}
