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

    public LinkedHashSet<Node> getValue(Node node) {
        return graph.get(node);
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

    public void deleteArch(Node firstNode, Node secondNode, Arch arch) {
        graph.get(firstNode).remove(secondNode);
        graph.get(secondNode).remove(firstNode);
        arch.delete();
    }

    public void deleteNode(Node node) {
        graph.remove(node);
        Vector<Arch> attachments = node.getAttachments();
        for(Arch arch : attachments) {
            arch.delete();
        }
        for(var it : graph.entrySet()) {
            it.getValue().remove(node);
        }
    }

    public void clear() {
        for(Node node : graph.keySet()) {
            for(Arch arch : node.getAttachments()) {
                arch.delete();
                node.deleteArch(arch);
            }
            graph.remove(node);
        }
    }

}
