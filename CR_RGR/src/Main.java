import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

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
        BorderPane root = rootInit(stage);

        stage.setTitle(title);
        stage.setWidth(720);
        stage.setHeight(440);

        root.getStylesheets().add("main/resources/main.css");

        stage.setScene(new Scene(root));
        stage.show();
    }

    private void reset(Pane drawSpace) {
        selectedNodes = null;
        graph.clear();
        graph = new Graph();
        drawSpace.getChildren().clear();
    }

    private BorderPane rootInit(Stage stage) {

        Pane drawSpace = new Pane();
        drawSpace.setId("drawSpace");

        VBox toolBar = toolPanelInit(stage, drawSpace);

        drawSpace.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            if(manualDraw_Mode == manualModes.NODES) {
                placeGraphNode(mouseEvent.getX(), mouseEvent.getY(), drawSpace);
            }
        });

        BorderPane root = new BorderPane();
        root.setCenter(drawSpace);
        root.setRight(toolBar);
        root.setId("root");

        return root;
    }

    private VBox toolPanelInit(Stage stage, Pane drawPanel) {
        Label tbLabel = new Label("Toolbar");
        tbLabel.setId("toolBarLabel");

        Button nodesMode = new Button("Установка\nузлов");
        nodesMode.setOnAction(actionEvent -> {
            manualDraw_Mode = manualModes.NODES;
        });

        Button archesMode = new Button("Установка\nдуг");
        archesMode.setOnAction(actionEvent -> {
            manualDraw_Mode = manualModes.ARCHES;
        });

        Button delMode = new Button("Удаление");
        delMode.setOnAction(actionEvent -> {
            manualDraw_Mode = manualModes.DELETE;
        });

        Button saveButton = new Button("Сохранить\nграф");
        saveButton.setOnAction(actionEvent -> {
            try {
                saveGraph(stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        Button loadButton = new Button("Загрузить\nграф");
        loadButton.setOnAction(actionEvent -> {
            try {
                loadGraph(stage, drawPanel);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        Button resetButton = new Button("Сбросить");
        resetButton.setOnAction(actionEvent -> {
            reset(drawPanel);
        });

        HBox modeSelectBox = new HBox(nodesMode, archesMode, delMode);
        modeSelectBox.setId("modeSelectBox");

        HBox savLoadResBox = new HBox(saveButton, loadButton, resetButton);
        modeSelectBox.setId("savLoadResBox");

        VBox toolBar = new VBox(tbLabel, modeSelectBox, savLoadResBox);
        toolBar.setId("toolBar");
        return toolBar;
    }


    private void placeGraphNode(double xCoord, double yCoord, Pane drowSpace) {
        Circle circleTmp = new Circle(xCoord, yCoord, nodesRadius);

        var coords = new Coords(xCoord, yCoord);

        drowSpace.getChildren().add(circleTmp);

        graph.addNode(new Node(coords, circleTmp, graph.len()));
    }
    private Pair<Node, Boolean> placeGraphNode(Coords coords, Pane drawSpace, int number) {
        Circle circleTmp = new Circle(coords.getX(), coords.getY(), nodesRadius);

        drawSpace.getChildren().add(circleTmp);
        Node node = new Node(coords, circleTmp, number);
        boolean success = graph.addNode(node);

        circleTmp.setOnMouseClicked(mouseEvent -> {
            switch (manualDraw_Mode) {
                case ARCHES -> {
                    node.select();
                    System.out.println("Clicked inside node " + node.getNumber() + ".");
                    if (selectedNodes == null || selectedNodes == node) {
                        selectedNodes = node;
                    } else {
                        doAttachment(selectedNodes, node, drawSpace);
                        selectedNodes = null;
                    }
                }
                case DELETE -> {
                    drawSpace.getChildren().remove(circleTmp);
                    graph.deleteNode(node);
                }
            }
        });
        return new Pair<>(node, success);
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

        lineTmp.setOnMouseClicked(mouseEvent -> {
            if(manualDraw_Mode == manualModes.DELETE) {
                drowSpace.getChildren().remove(lineTmp);
                graph.deleteArch(firstNode, secondNode, arch);
            }
        });

        drowSpace.getChildren().addFirst(lineTmp);
    }




    public void saveGraph(Stage stage) throws IOException {
        var now = java.time.LocalDateTime.now();
        System.out.println("Saving...");

        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранение графа");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Графовые файлы", "*.graph"));
        fc.setInitialFileName("Graph-Saved-"+now.getHour()+now.getMinute()+now.getSecond()+now.getNano());
        File saveFile = fc.showSaveDialog(stage);

        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(saveFile));
        } catch (NullPointerException e) {
            System.out.println("WARNING\t\tNot choose save path.");
            return;
        }

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

    public void loadGraph(Stage stage, Pane panel) throws FileNotFoundException {
        System.out.println("LOADING\t\tLoading...");
        Scanner sc;

        FileChooser fc = new FileChooser();
        fc.setTitle("Выберите файл графа");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Графовые файлы", "*.graph", "*.txt"));
        File choosenFile = fc.showOpenDialog(stage);
        try {
            sc = new Scanner(choosenFile);
            System.out.println("LOADING\t\tReading file.");
        } catch (FileNotFoundException e) {
            System.out.println("ERROR\t\tError loading! File not found.");
            return;
        } catch (NullPointerException e) {
            System.out.println("WARNING\t\tFile not choose.");
            return;
        }

        reset(panel);
        System.out.println("LOADING\t\tScreen has been cleared.\n");

        graph = new Graph();
        System.out.println("LOADING\t\tNew graph created.\n");

        sc.nextLine();
        System.out.println("LOADING\t\tSkip fst line.");

        while(sc.hasNextLine()) {
            String temp = sc.nextLine();
            System.out.println("LOADING\t\tGet new line.");

            int number = Character.getNumericValue(temp.charAt(0));
            System.out.println("LOADING\t\tGet node number - "+number);

            String[] coordsTxt = temp.substring(temp.lastIndexOf("["), temp.lastIndexOf("]")).replaceAll("[^\\d.\\s]", "").split(" ");
            Coords coords = new Coords(Double.parseDouble(coordsTxt[0]), Double.parseDouble(coordsTxt[1]));
            System.out.println("LOADING\t\tGet node coords: "+coords);

            var created = placeGraphNode(coords, panel, number);
            boolean success = created.getValue();
            Node nodeTemp = created.getKey();

            if(success) {
                System.out.println("LOADING\t\tNode "+nodeTemp.getNumber()+" has been created and added to graph.\n");
            }
            else {
                System.out.println("ERROR\t\tNode " + nodeTemp.getNumber() + " hasn't been created.\n");
            }

            String[] attachmentsTxt = temp.substring(temp.indexOf("["), temp.indexOf("]")).replaceAll("[^\\d.\\s]", "").split(" ");
            System.out.println("LOADING\t\tAttachments of node "+nodeTemp.getNumber()+":"+ Arrays.toString(attachmentsTxt));

            for (String attachNode : attachmentsTxt) {
                System.out.println("LOADING\t\tCheck attaches for "+nodeTemp.getNumber()+"...");

                if (attachNode != null && !attachNode.isEmpty()) {
                    Node secondNode = graph.findWithNum(Integer.parseInt(attachNode));
                    if (secondNode != null) {
                        System.out.println("LOADING\t\tLooking for " + attachNode);
                        doAttachment(nodeTemp, secondNode, panel);
                        System.out.println("LOADING\t\tSuccess attach " + nodeTemp.getNumber() + " and " + attachNode);
                    }
                    else {
                        System.out.println("WARNING\t\tAttachment nodes not yet created.");
                    }
                }
                else {
                    System.out.println("WARNING\t\tNode haven't attaches.");
                }
            }
        }
    }
}
