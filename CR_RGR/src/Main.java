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

import java.io.*;
import java.util.*;


public class Main extends Application {
    private static Graph graph = new Graph();
    private final static Vector<String> titlesVec = new Vector<>();
    private static String title = "ПАЛИТИКИ САШЛИСЬ В ДУЕЛИ!";

    static manualModes manualDraw_Mode = manualModes.NONE;
    static Node selectedNodes = null;

    private static final int nodesRadius = 30;

    private File choosenFile = null;

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

        titleUpdate(stage);
        stage.setMinWidth(640);
        stage.setMinHeight(360);

        root.getStylesheets().add("main/resources/main.css");

        stage.setScene(new Scene(root));
        stage.show();
    }

    private void titleUpdate(Stage stage) {
        if(choosenFile != null) {
            stage.setTitle(choosenFile.getName() + " | " + title);
        }
        else {
            stage.setTitle(title);
        }
    }

    private void reset(Pane drawSpace) {
        selectedNodes = null;
        graph = new Graph();
        drawSpace.getChildren().clear();
    }

    private BorderPane rootInit(Stage stage) {

        Pane drawSpace = new Pane();
        drawSpace.setId("drawSpace");

        VBox toolBar = toolPanelInit(stage, drawSpace);

        drawSpace.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            if(manualDraw_Mode == manualModes.NODES) {
                placeGraphNode(new Coords(mouseEvent.getX(), mouseEvent.getY()), drawSpace, graph.len());
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
        Button randomGraph = new Button("Случайный\nграф");
        randomGraph.setOnAction(actionEvent -> {
            randomGraph(5, 10, drawPanel);
        });

        HBox modeSelectBox = new HBox(nodesMode, archesMode, delMode);
        modeSelectBox.setId("modeSelectBox");

        HBox savLoadResBox = new HBox(saveButton, loadButton, resetButton);
        modeSelectBox.setId("savLoadResBox");

        VBox toolBar = new VBox(tbLabel, modeSelectBox, savLoadResBox, randomGraph);
        toolBar.setId("toolBar");
        return toolBar;
    }

    private Node placeGraphNode(Coords coords, Pane drawSpace, int number) {
        if(!graph.isNear(coords, nodesRadius*3)) {
            System.out.println("WARNING\t\tToo near to another node! Operation canceled.");
            return null;
        }

        Circle circleTmp = new Circle(coords.getX(), coords.getY(), nodesRadius);

        drawSpace.getChildren().add(circleTmp);
        Node node = new Node(coords, circleTmp, number);
        graph.addNode(node);


        circleTmp.setOnMouseClicked(mouseEvent -> {
            switch (manualDraw_Mode) {
                case manualModes.ARCHES: {
                    node.select();
                    System.out.println("Clicked inside node " + node.getNumber() + ".");
                    if (selectedNodes == null || selectedNodes == node) {
                        selectedNodes = node;
                    } else {
                        doAttachment(selectedNodes, node, drawSpace);
                        selectedNodes = null;
                    }
                    break;
                }
                case manualModes.DELETE: {
                    graph.deleteNode(node, drawSpace);
                    break;
                }
            }
        });
        return node;
    }

    private boolean doAttachment(Node firstNode, Node secondNode, Pane drawSpace) {
        firstNode.deSelect();
        secondNode.deSelect();

        boolean tryAttach = graph.addAttach(firstNode, secondNode);
        if(!tryAttach) {
            System.out.println("ERROR\t\tCannot attach.");
            return false;
        }

        Coords fstCenter = firstNode.getPos();
        Coords sndCenter = secondNode.getPos();

        Line lineTmp = new Line(fstCenter.getX(), fstCenter.getY(), sndCenter.getX(), sndCenter.getY());

        Arch arch = new Arch(firstNode, secondNode, lineTmp);
        firstNode.addAttachment(arch);
        secondNode.addAttachment(arch);

        lineTmp.setOnMouseClicked(mouseEvent -> {
            if(manualDraw_Mode == manualModes.DELETE) {
                graph.deleteArch(firstNode, secondNode, arch, drawSpace);
            }
        });

        drawSpace.getChildren().addFirst(lineTmp);

        return true;
    }


    public void randomGraph(final int nodesAmount, final int archesAmount, Pane drawSpace) {
        reset(drawSpace);
        Random random = new Random();
        for (int countNode = 0; countNode < nodesAmount; countNode++) {
            Node nodeTmp = null;
            while(nodeTmp == null) {
                double randomX = random.nextDouble(drawSpace.getWidth());
                double randomY = random.nextDouble(drawSpace.getHeight());
                Coords coords = new Coords(randomX, randomY);
                nodeTmp = placeGraphNode(coords, drawSpace, countNode);
            }
        }

        Vector<Node> nodes = graph.getNodes();

        for(int countArches = 0; countArches < archesAmount; countArches++) {
            boolean tryAttach = false;
            while(!tryAttach) {
                tryAttach = doAttachment(
                        nodes.get(random.nextInt(nodesAmount)),
                        nodes.get(random.nextInt(nodesAmount)),
                        drawSpace
                );
            }
        }
    }



    public void saveGraph(Stage stage) throws IOException {
        var now = java.time.LocalDateTime.now();
        System.out.println("Saving...");

        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранение графа");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Графовые файлы", "*.graph"));
        fc.setInitialFileName("Graph-Saved-"+now.getHour()+now.getMinute()+now.getSecond()+now.getNano());
        File saveFile = fc.showSaveDialog(stage);
        choosenFile = saveFile;

        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(saveFile));
        } catch (NullPointerException e) {
            System.out.println("WARNING\t\tNot choose save path.");
            return;
        }

        System.out.println("Saving...");
        writer.write("FILE_TYPE-GRAPH");
        writer.newLine();
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
        titleUpdate(stage);
        System.out.println("Saved done");
    }

    public void loadGraph(Stage stage, Pane panel) throws FileNotFoundException {
        System.out.println("LOADING\t\tLoading...");
        Scanner sc;

        FileChooser fc = new FileChooser();
        fc.setTitle("Выберите файл графа");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Графовые файлы", "*.graph", "*.txt"));
        try {
            fc.setInitialDirectory(choosenFile.getParentFile());
        } catch (NullPointerException e) {
            System.out.println("WARNING\t\tBuffer haven't file. Init select new fiile.");
        }
        choosenFile = fc.showOpenDialog(stage);
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

        String fileTypeCheck = sc.nextLine();
        if(!Objects.equals(fileTypeCheck, "FILE_TYPE-GRAPH")) {
            System.out.println("ERROR\t\tFile does not exists, wrong type.");
            return;
        }

        reset(panel);
        System.out.println("LOADING\t\tScreen has been cleared.\n");

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

            Node nodeTemp = placeGraphNode(coords, panel, number);
            System.out.println("LOADING\t\tNode "+nodeTemp.getNumber()+" has been created and added to graph.\n");

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
        titleUpdate(stage);
    }
}
