import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

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
}
