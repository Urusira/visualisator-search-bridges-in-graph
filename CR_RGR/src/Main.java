import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;


public class Main extends Application {
    private static Graph graph = new Graph();
    private final static Vector<String> titlesVec = new Vector<>();
    private static String title;

    static manualModes manualDraw_Mode = manualModes.NONE;
    static Node selectedNodes = null;

    private static double nodesRadius = 30;
    private static double minNodesDist = nodesRadius*3;

    private File choosenFile = null;

    private static Stage mainStage;
    private static Pane drawSpace;

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
        mainStage = stage;

        BorderPane root = rootInit();

        titleUpdate();
        stage.setMinWidth(640);
        stage.setMinHeight(360);
        stage.setMaximized(true);

        root.getStylesheets().add("main/resources/main.css");

        stage.setScene(new Scene(root));
        stage.show();
    }

    private void titleUpdate() {
        if(choosenFile != null) {
            mainStage.setTitle(choosenFile.getName() + " | " + title);
        }
        else {
            mainStage.setTitle(title);
        }
    }

    private void reset() {
        loggerPush("RESET\t\tStart resetting draw panel.");
        selectedNodes = null;
        graph = new Graph();
        loggerPush("RESET\t\tGraph has cleared.");
        drawSpace.getChildren().clear();
        loggerPush("RESET\t\tPanel has cleared.");
    }

    private BorderPane rootInit() {
        drawSpace = new Pane();
        drawSpace.setId("drawSpace");

        Tab graphBuildTab = new Tab("Graph building", graphBuilderInit());
        graphBuildTab.closableProperty().set(false);
        Tab algorythmTab = new Tab("Bridges search", bridgeSearchInit());
        algorythmTab.closableProperty().set(false);
        TabPane rightPanel = new TabPane(graphBuildTab, algorythmTab);

        ToolBar toolBar = toolBarInit();

        drawSpace.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            if(manualDraw_Mode == manualModes.NODES) {
                placeGraphNode(new Coords(mouseEvent.getX(), mouseEvent.getY()), graph.len());
            }
        });

        BorderPane root = new BorderPane();
        root.setCenter(drawSpace);
        root.setRight(rightPanel);
        root.setTop(toolBar);
        root.setId("root");

        return root;
    }

    private VBox graphBuilderInit() {
        Label tbLabel = new Label("Manual building graph");
        tbLabel.setId("rightPanelLabel");

        Button nodesMode = new Button("Set\nnodes");
        nodesMode.setOnAction(actionEvent -> {
            manualDraw_Mode = manualModes.NODES;
        });

        Button archesMode = new Button("Set\narches");
        archesMode.setOnAction(actionEvent -> {
            manualDraw_Mode = manualModes.ARCHES;
        });

        Button delMode = new Button("Delete");
        delMode.setOnAction(actionEvent -> {
            manualDraw_Mode = manualModes.DELETE;
        });

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(actionEvent -> {
            reset();
        });

        Label randomLabel = new Label("Random graph generation");
        TextField nodesAmount = new TextField();
        TextField archesAmount = new TextField();

        nodesAmount.setPromptText("Nodes amount");
        archesAmount.setPromptText("Arches amount");
        HBox randomParams = new HBox(nodesAmount, archesAmount);
        Button randomGraph = new Button("Generate");
        randomGraph.setOnAction(actionEvent -> {
            randomGraph(Integer.parseInt(nodesAmount.getText()), Integer.parseInt(archesAmount.getText()));
        });

        HBox modeSelectBox = new HBox(nodesMode, archesMode, delMode);
        modeSelectBox.setId("modeSelectBox");

        VBox graphBuildTab = new VBox(tbLabel, modeSelectBox, resetButton , randomLabel, randomParams, randomGraph);
        graphBuildTab.setId("graphBuildTab");
        return graphBuildTab;
    }

    //TODO: ЗДЕСЬ РАСПОЛАГАЕТСЯ ИНИЦИАЛИЗАЦИЯ ПАНЕЛИ АЛГОРИТМА. В НЕЙ НУЖНО РЕАЛИЗОВАТЬ ВЕСЬ АЛГОРИТМ ПОИСКА МОСТА.
    private VBox bridgeSearchInit() {
        Label algSpeedLabel = new Label("Speed of visualisation");
        Slider algorythmSpeed = new Slider(0d, 100, 0.1);
        algorythmSpeed.setShowTickLabels(true);
        algorythmSpeed.setShowTickMarks(true);


        Button startRun = new Button("Start graph run");
        startRun.setId("startAlgo");
        startRun.setOnAction(actionEvent -> {
            try {
                runInDepth(algorythmSpeed.getValue());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        VBox algorythmTab = new VBox(algSpeedLabel, algorythmSpeed, startRun);
        algorythmTab.setId("algorythmTab");
        return algorythmTab;
    }

    private void runInDepth(double visSpeed) throws InterruptedException {
        Node curNode = graph.findWithNum(0);
        runNext(curNode, visSpeed);
    }

    private synchronized void runNext(final Node curNode, double visSpeed) throws InterruptedException {
        curNode.turnHighlight();
        PauseTransition nodePause = new PauseTransition(Duration.seconds(1/visSpeed));
        nodePause.setOnFinished(actionEvent -> {
            Vector<Arch> attaches = curNode.getAttachments();
            Optional<Arch> attachEmptyCheck = Optional.empty();
            for(Arch arch : attaches) {
                if(!arch.isVisited()) {
                    attachEmptyCheck = Optional.of(arch);
                }
            }
            if(attachEmptyCheck.isPresent()) {
                attachEmptyCheck.get().turnHighlight();
                curNode.turnHighlight();
                curNode.visit();
                final Arch attach = attachEmptyCheck.get();
                PauseTransition archPause = getArchPause(attach, visSpeed);
                archPause.play();
            }
            else {
                return;
            }
        });
        nodePause.play();
        return;
    }

    private PauseTransition getArchPause(Arch attach, final double visSpeed) {
        PauseTransition archPause = new PauseTransition(Duration.seconds(1/visSpeed));
        archPause.setOnFinished(actionEvent2 -> {
            attach.turnHighlight();
            attach.visit();
            Node[] nodes = attach.getTransitNodes();
            if(nodes[0].isVisited()) {
                try {
                    runNext(nodes[1], visSpeed);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                try {
                    runNext(nodes[0], visSpeed);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return archPause;
    }

    private ToolBar toolBarInit() {
        MenuBar menuBar = new MenuBar();

        ToolBar toolBar = new ToolBar(menuBar);
        toolBar.setOrientation(Orientation.HORIZONTAL);

        Menu fileMenu = new Menu("File");

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(actionEvent -> {
            try {
                saveGraph(false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        MenuItem saveAsItem = new MenuItem("Save as...");
        saveAsItem.setOnAction(actionEvent -> {
            try {
                saveGraph(true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        MenuItem loadItem = new MenuItem("Load");
        loadItem.setOnAction(actionEvent -> {
            try {
                loadGraph();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        fileMenu.getItems().addAll(saveItem, saveAsItem, loadItem);

        Menu optionsMenu = new Menu("Options");

        MenuItem minDistNodes = new MenuItem("Min. distance");
        minDistNodes.setOnAction(actionEvent -> insertMinDist());

        MenuItem nodesRadius = new MenuItem("Nodes radius");
        nodesRadius.setOnAction(actionEvent -> insertRadiusNodes());

        optionsMenu.getItems().addAll(minDistNodes, nodesRadius);


        menuBar.getMenus().addAll(fileMenu, optionsMenu);
        return toolBar;
    }

    private void insertMinDist() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(minNodesDist));
        dialog.setTitle("Insert minimal distance");
        dialog.setHeaderText("Please, insert new minimal distance between nodes.");
        dialog.setContentText("Minimal distance:");
        dialog.showAndWait().ifPresent(newValue -> {
            if(Double.parseDouble(newValue) > 0) {
                minNodesDist = Double.parseDouble(newValue);
            } else {
                wrongInputAlert("Input value must be only greater than 0!");
            }
        });
    }

    private void insertRadiusNodes() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(nodesRadius));
        dialog.setTitle("Insert nodes radius");
        dialog.setHeaderText("Please, insert new radius of nodes.");
        dialog.setContentText("Radius (7 - min):");
        dialog.showAndWait().ifPresent(newValue -> {
            if(Double.parseDouble(newValue) >= 7d) {
                double old = nodesRadius;
                nodesRadius = Double.parseDouble(newValue);
                double div = old-nodesRadius;
                for(Node node : graph.getNodes()) {
                    StackPane cont = node.getContainer();
                    if(node.getFigure() instanceof Circle circle) {
                        circle.setRadius(nodesRadius);
                        cont.setLayoutX(cont.getLayoutX()+div);
                        cont.setLayoutY(cont.getLayoutY()+div);
                    }
                }
            } else {
                wrongInputAlert("Input value must be greater than 7!");
            }
        });
    }

    private Node placeGraphNode(Coords coords, int number) {
        if(!graph.isNear(coords, minNodesDist)) {
            loggerPush("WARNING\t\tToo near to another node! Operation canceled.");
            return null;
        }

        Circle circleTmp = new Circle(coords.getX(), coords.getY(), nodesRadius);
        StackPane circleStack = new StackPane(circleTmp, new Text(Integer.toString(number)));
        circleStack.setShape(circleTmp);
        circleStack.setLayoutX(coords.getX()-nodesRadius);
        circleStack.setLayoutY(coords.getY()-nodesRadius);

        drawSpace.getChildren().add(circleStack);
        Node node = new Node(coords, circleStack, number);
        graph.addNode(node);


        circleStack.setOnMouseClicked(mouseEvent -> {
            switch (manualDraw_Mode) {
                case manualModes.ARCHES: {
                    node.select();
                    loggerPush("Clicked inside node " + node.getNumber() + ".");
                    if (selectedNodes == null || selectedNodes == node) {
                        selectedNodes = node;
                    } else {
                        doAttachment(selectedNodes, node);
                    }
                    break;
                }
                case manualModes.DELETE: {
                    //TODO: ПРИ УДАЛЕНИИ НАДО ОБНОВИТЬ НУМЕРАЦИЮ ВСЕХ ВЕРШИН ДЛЯ ИЗБЕЖАНИЯ ДЫР
                    graph.deleteNode(node, drawSpace);
                    break;
                }
            }
        });
        return node;
    }

    private boolean doAttachment(Node firstNode, Node secondNode) {
        selectedNodes = null;

        loggerPush("DO_ATTACH\t\tDeselection nodes.");
        firstNode.deSelect();
        secondNode.deSelect();

        loggerPush("DO_ATTACH\t\tTrying add attach in links table.");
        boolean tryAttach = graph.addAttach(firstNode, secondNode);
        if(!tryAttach) {
            loggerPush("ERROR\t\tCannot attach.");
            return false;
        }

        loggerPush("DO_ATTACH\t\tGet coordinates, create line.");
        Coords fstCenter = firstNode.getPos();
        Coords sndCenter = secondNode.getPos();

        Line lineTmp = new Line(fstCenter.getX(), fstCenter.getY(), sndCenter.getX(), sndCenter.getY());

        loggerPush("DO_ATTACH\t\tCreating arch.");
        Arch arch = new Arch(firstNode, secondNode, lineTmp);

        loggerPush("DO_ATTACH\t\tAdd attachments in nodes.");
        firstNode.addAttachment(arch);
        secondNode.addAttachment(arch);

        loggerPush("DO_ATTACH\t\tCreate mouse click handler.");
        lineTmp.setOnMouseClicked(mouseEvent -> {
            if(manualDraw_Mode == manualModes.DELETE) {
                graph.deleteArch(firstNode, secondNode, arch, drawSpace);
            }
        });

        loggerPush("DO_ATTACH\t\tAdd line to draw panel.");
        drawSpace.getChildren().addFirst(lineTmp);

        loggerPush("DO_ATTACH\t\tSuccessful attach.");
        return true;
    }


    public void randomGraph(final int nodesAmount, int archesAmount) {
        loggerPush("RANDOM_GEN\t\tStart generating random graph.");

        reset();
        Random random = new Random();
        for (int countNode = 0; countNode < nodesAmount; countNode++) {
            Node nodeTmp = null;
            // TODO: МОЖНО УПРОСТИТЬ, ВМЕСТО ПОПЫТКИ УСТАНОВКИ НОДЫ ВЫПОЛНИТЬ ПРОВЕРКУ ДОСТУПНОСТИ КООРДИНАТ
            while(nodeTmp == null) {
                double randomX = random.nextDouble(drawSpace.getWidth());
                double randomY = random.nextDouble(drawSpace.getHeight());
                Coords coords = new Coords(randomX, randomY);
                nodeTmp = placeGraphNode(coords, countNode);
            }
        }

        Vector<Node> nodes = graph.getNodes();

        int max = ((nodesAmount-1)*nodesAmount)/2;

        if(archesAmount > max) {

            archesAmount = max;
        }

        for(int countArches = 0; countArches < archesAmount; countArches++) {
            boolean tryAttach = false;
            while(!tryAttach) {
                tryAttach = doAttachment(
                        nodes.get(random.nextInt(nodesAmount)),
                        nodes.get(random.nextInt(nodesAmount))
                );
            }
        }
    }



    public void saveGraph(boolean asNew) throws IOException {
        var now = java.time.LocalDateTime.now();
        loggerPush("Saving...");

        if(asNew) {
            FileChooser fc = new FileChooser();
            fc.setTitle("Graph save");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Graphs files", "*.graph"));
            fc.setInitialFileName("Graph-Saved-" + now.getHour() + now.getMinute() + now.getSecond() + now.getNano());
            choosenFile = fc.showSaveDialog(mainStage);
        }

        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(choosenFile));
        } catch (NullPointerException e) {
            loggerPush("WARNING\t\tNot choose save path.");
            if(!asNew) {
                loggerPush("SAVING\t\tChoose new save path.");
                saveGraph(true);
            }
            return;
        }

        loggerPush("SAVING\t\tSaving init");
        writer.write("FILE_TYPE-GRAPH");
        writer.newLine();
        writer.write(String.valueOf(now));
        writer.newLine();

        for(Node node : graph.getKeys()) {
            loggerPush("SAVING\t\t"+((double)(node.getNumber()+1)/graph.len())*100+"%");
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
        titleUpdate();
        loggerPush("SAVING\t\tSaved done");
    }

    public void loadGraph() throws FileNotFoundException {
        loggerPush("LOADING\t\tLoading...");
        Scanner sc;

        FileChooser fc = new FileChooser();
        fc.setTitle("Selection graph file");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Graphs files", "*.graph", "*.txt"));
        try {
            fc.setInitialDirectory(choosenFile.getParentFile());
        } catch (NullPointerException e) {
            loggerPush("WARNING\t\tBuffer haven't file. Init select new fiile.");
        }
        choosenFile = fc.showOpenDialog(mainStage);
        try {
            sc = new Scanner(choosenFile);
            loggerPush("LOADING\t\tReading file.");
        } catch (FileNotFoundException e) {
            loggerPush("ERROR\t\tError loading! File not found.");
            return;
        } catch (NullPointerException e) {
            loggerPush("WARNING\t\tFile not choose.");
            return;
        }

        String fileTypeCheck = sc.nextLine();
        if(!Objects.equals(fileTypeCheck, "FILE_TYPE-GRAPH")) {
            loggerPush("ERROR\t\tFile does not exists, wrong type.");
            return;
        }

        reset();
        loggerPush("LOADING\t\tScreen has been cleared.\n");

        sc.nextLine();
        loggerPush("LOADING\t\tSkip fst line.");

        while(sc.hasNextLine()) {
            String loadedLine = sc.nextLine();
            loggerPush("LOADING\t\tGet new line.");

            String[] coordTxt = loadedLine.substring(loadedLine.lastIndexOf("["), loadedLine.lastIndexOf("]")).replaceAll("[^\\d.\\s]", "").split(" ");

            int number = Integer.parseInt(loadedLine.substring(0, loadedLine.indexOf(" ")));
            loggerPush("LOADING\t\tGet node number - "+number);

            Coords coords = new Coords(Double.parseDouble(coordTxt[0]), Double.parseDouble(coordTxt[1]));
            loggerPush("LOADING\t\tGet node coords: "+coords);


            Node nodeTemp = placeGraphNode(coords, number);
            loggerPush("LOADING\t\tNode "+nodeTemp.getNumber()+" has been created and added to graph.\n");

            String[] attachmentsTxt = loadedLine.substring(loadedLine.indexOf("["), loadedLine.indexOf("]")).replaceAll("[^\\d.\\s]", "").split(" ");
            loggerPush("LOADING\t\tAttachments of node "+nodeTemp.getNumber()+":"+ Arrays.toString(attachmentsTxt));

            for (String attachNode : attachmentsTxt) {
                loggerPush("LOADING\t\tCheck attaches for "+nodeTemp.getNumber()+"...");

                if (attachNode != null && !attachNode.isEmpty()) {
                    Node secondNode = graph.findWithNum(Integer.parseInt(attachNode));
                    if (secondNode != null) {
                        loggerPush("LOADING\t\tLooking for " + attachNode);
                        doAttachment(nodeTemp, secondNode);
                        loggerPush("LOADING\t\tSuccess attach " + nodeTemp.getNumber() + " and " + attachNode);
                    }
                    else {
                        loggerPush("WARNING\t\tAttachment nodes not yet created.");
                    }
                }
                else {
                    loggerPush("WARNING\t\tNode haven't attaches.");
                }
            }
        }
        titleUpdate();
    }

    private void wrongInputAlert(String desc) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Wrong input");
        alert.setHeaderText(null);
        alert.setContentText(desc);
        alert.setOnCloseRequest(dialogEvent -> {
            insertRadiusNodes();
        });
        alert.showAndWait();
    }
    
    //TODO: ЗДЕСЬ БУДЕТ ВЫВОД ТЕКСТА В ПАНЕЛЬКУ ВНИЗУ ПРОГРАММЫ
    private void loggerPush(String text) {
        System.out.println(text);
    }
}
