import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;


public class Main extends Application {
    private static Graph graph = new Graph();
    private final static Vector<String> titlesVec = new Vector<>();
    private static String title = "ПАЛИТИКИ САШЛИСЬ В ДУЕЛИ!";

    static manualModes manualDraw_Mode = manualModes.NONE;
    static Node selectedNodes = null;

    private static final int nodesRadius = 30;



    public static void main(String[] args) throws FileNotFoundException {
        Scanner sc = new Scanner(new File("titles.txt"));
        while (sc.hasNextLine()) {
            titlesVec.add(sc.nextLine());
        }

        Random rd = new Random();
        title = titlesVec.get(rd.nextInt(titlesVec.size()));

        launch(args);
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = rootInit();

        stage.setTitle(title);
        stage.setWidth(720);
        stage.setHeight(440);

        root.getStylesheets().add("main/resources/main.css");

        stage.setScene(new Scene(root));
        stage.show();
    }



    private BorderPane rootInit() {

        Pane drawSpace = new Pane();
        drawSpace.setId("drawSpace");

        VBox toolBar = toolPanelInit(drawSpace);

        drawSpace.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> clicksHandler(drawSpace, mouseEvent));

        BorderPane root = new BorderPane();
        root.setCenter(drawSpace);
        root.setRight(toolBar);
        root.setId("root");

        return root;
    }

    private VBox toolPanelInit(Pane drawPanel) {
        Label tbLabel = new Label("Toolbar");
        tbLabel.setId("toolBarLabel");

        Button changeModeButton = new Button("Установка\nузлов");
        changeModeButton.setOnAction(actionEvent -> {
            changeMode();
            switch (manualDraw_Mode) {
                case NODES -> changeModeButton.setText("Соединение\nузлов");
                case ARCHES -> changeModeButton.setText("Установка\nузлов");
            }
        });

        Button saveButton = new Button("Сохранить\nграф");
        saveButton.setOnAction(actionEvent -> {
            try {
                saveGraph();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        Button loadButton = new Button("Загрузить\nграф");
        loadButton.setOnAction(actionEvent -> {
            try {
                loadGraph(drawPanel);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        VBox toolBar = new VBox(tbLabel, changeModeButton, saveButton, loadButton);
        toolBar.setId("toolBar");
        return toolBar;
    }



    private void placeGraphNode(double xCoord, double yCoord, Pane drowSpace) {
        Circle circleTmp = new Circle(xCoord, yCoord, nodesRadius);

        var coords = new Coords(xCoord, yCoord);
        graph.addNode(new Node(coords, circleTmp, graph.len()));

        drowSpace.getChildren().add(circleTmp);
    }

    private void doAttachment(Node firstNode, Node secondNode, Pane drowSpace) {
        firstNode.deSelect();
        secondNode.deSelect();

        boolean tryAddToGraph = graph.getValue(firstNode).add(secondNode) && graph.getValue(secondNode).add(firstNode);
        if(!tryAddToGraph) {
            System.out.println("Has attached! Operation is canceled.");
            return;
        }

        Coords fstCenter = firstNode.getPos();
        Coords sndCenter = secondNode.getPos();

        Line lineTmp = new Line(fstCenter.getX(), fstCenter.getY(), sndCenter.getX(), sndCenter.getY());

        Arch arch = new Arch(firstNode, secondNode, lineTmp);
        firstNode.addAttachment(arch);
        secondNode.addAttachment(arch);

        drowSpace.getChildren().addFirst(lineTmp);
    }





    private void clicksHandler(Pane drawSpace, MouseEvent mouseEvent) {
        double clX = mouseEvent.getX();
        double clY = mouseEvent.getY();

        System.out.println("Mouse click X: "+clX+"; Mouse click X: "+clY+"; Actual mode: "+ manualDraw_Mode.name());

        switch (manualDraw_Mode) {
            case NODES: {
                placeGraphNode(clX, clY, drawSpace);
                break;
            }
            case ARCHES: {
                for(var node : graph.getKeys()) {
                    Circle circle = (Circle) node.getFigure();
                    Coords circCenter = new Coords(circle.getCenterX(), circle.getCenterY());

                    double evcliDist = Math.sqrt(Math.pow(clX-circCenter.getX(), 2)+Math.pow(clY-circCenter.getY(), 2));

                    if(evcliDist <= circle.getRadius()) {
                        node.select();
                        System.out.println("Clicked inside node "+node.getNumber()+".");
                        if(selectedNodes == null || selectedNodes == node) {
                            selectedNodes = node;
                        }
                        else {
                            doAttachment(selectedNodes, node, drawSpace);
                            selectedNodes = null;
                        }
                    }
                    else {
                        node.deSelect();
                    }
                }
            }
        }
    }




    public static void changeMode() {
        switch (manualDraw_Mode) {
            case NONE, ARCHES -> manualDraw_Mode = manualModes.NODES;
            case NODES -> manualDraw_Mode = manualModes.ARCHES;
        }
    }



    public void saveGraph() throws IOException {
        var now = java.time.LocalDateTime.now();
        System.out.println("Saving...");

        BufferedWriter writer = new BufferedWriter(new FileWriter(
                "Graph-Saved-"+now.getHour()+now.getMinute()+now.getSecond()+now.getNano()+".txt",
                true)
        );
        System.out.println("Saving...");
        writer.write(String.valueOf(now));
        writer.newLine();

        for(Node node : graph.getKeys()) {
            System.out.println(((double)(node.getNumber()+1)/graph.len())*100+"%");
            writer.write(
                    node.getNumber()+
                            " attached["+
                            graph.getStrValues(node)+
                            "], coords["+
                            node.getPos().toString()+
                            "]"
                    );
            writer.newLine();
        }
        writer.close();
        System.out.println("Saved done");
    }

    public void loadGraph(Pane panel) throws FileNotFoundException {
        Scanner sc;
        try {
            sc = new Scanner(new File("GraphLoad.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("Error loading! File not found.");
            return;
        }

        while(sc.hasNextLine()) {
            sc.nextLine();

            String temp = sc.nextLine();

            int number = Character.getNumericValue(temp.charAt(0));

            String[] coordsTxt = temp.substring(temp.lastIndexOf("["), temp.lastIndexOf("]")).replaceAll("[^\\d.\\s]", "").split(" ");
            Coords coords = new Coords(Double.parseDouble(coordsTxt[0]), Double.parseDouble(coordsTxt[1]));

            Node nodeTemp = new Node(coords, new Circle(coords.getX(), coords.getY(), nodesRadius), number);
            graph.addNode(nodeTemp);

            String[] attachmentsTxt = temp.substring(temp.indexOf("["), temp.indexOf("]")).replaceAll("[^\\d.\\s]", "").split(" ");
            graph = new Graph();

            for(int j = 0; j < attachmentsTxt.length; j++) {
                Node secondNode = graph.findWithNum(number);

                if(secondNode != null) {
                    doAttachment(nodeTemp, graph.findWithNum(number), panel);
                }
            }
        }
    }
}
