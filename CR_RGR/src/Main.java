import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


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

    private static Vector<Pair<Node, Color>> algoStack;
    private static Vector<Pair<Node, Color>> reverseAlgoStack;
    private static final Vector<Arch> bridges = new Vector<>();

    private static AnchorPane algoPanelBlocker;
    private final AtomicReference<Button> algoButtonNext = new AtomicReference<>();
    private final AtomicReference<Button> algoButtonPrev = new AtomicReference<>();

    private final AtomicInteger actualStep = new AtomicInteger(0);

    private int timer = 0;

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
        graphBuildTab.setOnSelectionChanged(_ -> {
            algoPanelBlocker.setVisible(true);
            timer = 0;
            graph.getNodes().forEach(node -> {
                node.setTin(-1);
                node.setLow(-1);
                node.setColor(Color.WHITE);
                node.turnOffHighlight();
            });
            actualStep.set(0);
            bridges.forEach(arch -> {if(arch!=null) arch.turnOffHighlight();});
            try{
                algoStack.clear();
                reverseAlgoStack.clear();
                bridges.clear();
            } catch (NullPointerException _){}
            algoButtonNext.get().setDisable(false);
            algoButtonPrev.get().setDisable(true);
        });

        Tab algorythmTab = new Tab("Bridges search", algoPanelInit());
        algorythmTab.closableProperty().set(false);
        algorythmTab.setOnSelectionChanged(_ -> {
            manualDraw_Mode = manualModes.NONE;
        });

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
            int nodAmoInt;
            int archAmoInt;

            try {
                nodAmoInt = Integer.parseInt(nodesAmount.getText());
            } catch (NumberFormatException e) {
                wrongInputAlert("Insert nodes amount!");
                return;
            }

            int maxArches = ((nodAmoInt-1)*nodAmoInt)/2;

            if(archesAmount.getText().isEmpty()) {archAmoInt = maxArches;}
            else {
                try {
                    archAmoInt = Integer.parseInt(archesAmount.getText());
                    if(archAmoInt > maxArches) {archAmoInt = maxArches;}
                } catch (NumberFormatException e) {
                    wrongInputAlert("Incorrect input");
                    return;
                }
            }
            if (nodAmoInt >= 0 && archAmoInt >= 0) {
                randomGraph(nodAmoInt, archAmoInt);
            }
            else {
                wrongInputAlert("Nodes amount and arches amount cannot be zero!");
            }
        });

        HBox modeSelectBox = new HBox(nodesMode, archesMode, delMode);
        modeSelectBox.setId("modeSelectBox");

        VBox graphBuildTab = new VBox(tbLabel, modeSelectBox, resetButton , randomLabel, randomParams, randomGraph);
        graphBuildTab.setId("graphBuildTab");
        return graphBuildTab;
    }

    private StackPane algoPanelInit() {
        Label algSpeedLabel = new Label("Speed of visualisation");
        Slider algorithmSpeed = new Slider(1d, 10, 1);
        algorithmSpeed.setShowTickLabels(true);
        algorithmSpeed.setShowTickMarks(true);

        Button startRun = new Button("Start auto");
        startRun.setId("startAlgo");

        Button previous = new Button("Previous");
        previous.setDisable(true);
        algoButtonPrev.set(previous);

        Button next = new Button("Next");
        algoButtonNext.set(next);

        startRun.setOnAction(actionEvent -> {
            previous.setDisable(true);
            next.setDisable(true);
            Timeline timeline = new Timeline();

            for(int i = 0; i < algoStack.size()-1; i++) {
                final int index = i;
                KeyFrame keyFrame1 = new KeyFrame(Duration.millis(
                        index*(1000/algorithmSpeed.getValue())+(1000/algorithmSpeed.getValue())), actionEvent2 -> {
                                nextStep();

                        });
                timeline.getKeyFrames().add(keyFrame1);
            }
            timeline.setOnFinished(_ -> {
                previous.setDisable(false);
            });
            timeline.play();
        });

        previous.setOnAction(actionEvent -> {
            next.setDisable(false);
            backStep();
            if(actualStep.get() <= 0) {
                previous.setDisable(true);
                actualStep.set(0);
            }
        });

        next.setOnAction(actionEvent -> {
            previous.setDisable(false);
            nextStep();
            if(actualStep.get() >= algoStack.size()-1) {
                next.setDisable(true);
            }
        });


        HBox manualSteps = new HBox(previous, next);

        AnchorPane overlay = new AnchorPane();
        overlay.setId("algorithmBlocker");
        overlay.setPickOnBounds(true);

        Button startButton = new Button("Start");
        startButton.setOnAction(actionEvent -> {
            try {
                algoStack = new Vector<>();
                stepInDepth(graph.getNodes().getFirst(), null);
                reverseAlgoStack = revResortStack(algoStack);
                graph.getNodes().getFirst().turnOnHighlight();
                overlay.setVisible(false);
            } catch (NoSuchElementException e) {
                wrongInputAlert("At first create/load/generate graph!");
            }
        });

        StackPane wrapStartButton = new StackPane(startButton);
        wrapStartButton.setAlignment(Pos.CENTER);
        AnchorPane.setTopAnchor(wrapStartButton, 0.0);
        AnchorPane.setBottomAnchor(wrapStartButton, 0.0);
        AnchorPane.setLeftAnchor(wrapStartButton, 0.0);
        AnchorPane.setRightAnchor(wrapStartButton, 0.0);

        overlay.getChildren().add(wrapStartButton);
        algoPanelBlocker = overlay;

        VBox algoContolPanel = new VBox(algSpeedLabel, algorithmSpeed, startRun, manualSteps);
        algoContolPanel.setId("algoContolPanel");

        return new StackPane(algoContolPanel, overlay);
    }

    private void backStep() {
        if(actualStep.get() >= algoStack.size()-1) {
            actualStep.set(algoStack.size()-1);
        }
        Node node = reverseAlgoStack.get(actualStep.get()).getKey();
        node.turnOffHighlight();
        actualStep.getAndDecrement();
        node = reverseAlgoStack.get(actualStep.get()).getKey();
        Color color = reverseAlgoStack.get(actualStep.get()).getValue();
        if (color != null){
            node.setColor(color);
        }
        node.turnOnHighlight();
        if(actualStep.get() < reverseAlgoStack.size()-1 && bridges.get(actualStep.get()+1) != null) {
            bridges.get(actualStep.get()+1).turnOffHighlight();
        }
    }

    private void nextStep() {
        if(actualStep.get() >= algoStack.size()-1) {
            actualStep.set(algoStack.size()-1);
        }
        Node node = algoStack.get(actualStep.get()).getKey();
        node.turnOffHighlight();
        actualStep.getAndIncrement();
        node = algoStack.get(actualStep.get()).getKey();
        Color color = algoStack.get(actualStep.get()).getValue();
        if(color != null){
            node.setColor(color);
        }
        node.turnOnHighlight();
        if(bridges.get(actualStep.get()) != null) {
            bridges.get(actualStep.get()).turnOnHighlight();
        }
    }

    //TODO: добавить отображение структур данных. Короче говоря - отобразить метки времени у узлов
    private void stepInDepth(final Node curNode, final Node parent) {
        curNode.setLow(timer);
        curNode.setTin(timer);
        timer++;
        curNode.setHiddenColor(Color.GRAY);
        algoStack.add(new Pair<>(curNode, null));
        algoStack.add(new Pair<>(curNode, Color.GRAY));
        bridges.add(null);
        bridges.add(null);

        Vector<Node> nodes = graph.getAttaches(curNode);
        for(Node to : nodes) {
            if(to.equals(parent)) continue;
            if(to.getTin() == -1) {
                stepInDepth(to, curNode);

                curNode.setLow(Integer.min(curNode.getLow(), to.getLow()));

                if(to.getLow() > curNode.getTin()) {
                    bridges.add(algoStack.size(), graph.findArch(curNode, to));
                }
            }
            else {
                curNode.setLow(Integer.min(curNode.getLow(), to.getTin()));
            }
            if(!algoStack.getLast().getKey().equals(curNode)) {
                algoStack.add(new Pair<>(curNode, null));
                bridges.add(null);
            }
        }
        curNode.setHiddenColor(Color.BLACK);
        algoStack.add(new Pair<>(curNode, Color.BLACK));
        bridges.add(null);
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
                loadGraph(false);
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

    //TODO: отображать метки времени у узлов
    private Node placeGraphNode(Coords coords, int number) {
        if(graph.isNear(coords, minNodesDist)) {
            loggerPush("WARNING\t\tToo near to another node! Operation canceled.");
            return null;
        }

        Circle circleTmp = new Circle(coords.getX(), coords.getY(), nodesRadius);
        StackPane circleStack = new StackPane(circleTmp);
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

    private void randomGraph(final int nodesAmount, int archesAmount) {
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

    private void saveGraph(boolean asNew) throws IOException {
        var now = LocalDateTime.now();
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

        writer.write(graph.toString());
        writer.newLine();
        writer.close();
        titleUpdate();
        loggerPush("SAVING\t\tSaved done");
    }

    private void loadGraph(boolean reload) throws FileNotFoundException {
        algoPanelBlocker.setVisible(true);

        loggerPush("LOADING\t\tLoading...");
        Scanner sc;

        if(!reload){
            FileChooser fc = new FileChooser();
            fc.setTitle("Selection graph file");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Graphs files", "*.graph", "*.txt"));
            try {
                fc.setInitialDirectory(choosenFile.getParentFile());
            } catch (NullPointerException e) {
                loggerPush("WARNING\t\tBuffer haven't file. Init select new file.");
            }
            choosenFile = fc.showOpenDialog(mainStage);
        }
        try {
            sc = new Scanner(choosenFile);
            loggerPush("LOADING\t\tReading file.");
        } catch (FileNotFoundException e) {
            loggerPush("ERROR\t\tError loading! File not found.");
            return;
        } catch (NullPointerException e) {
            loggerPush("WARNING\t\tFile not choose.");
                                                                            //TODO: Дебаговая штука, удалить в финале
                                                                            if(reload) {
                                                                                loadGraph(false);
                                                                            }
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

            String[] cordTxt = loadedLine.substring(loadedLine.lastIndexOf("["), loadedLine.lastIndexOf("]")).replaceAll("[^\\d.\\s]", "").split(" ");

            int number = Integer.parseInt(loadedLine.substring(0, loadedLine.indexOf(" ")));
            loggerPush("LOADING\t\tGet node number - "+number);

            Coords coords = new Coords(Double.parseDouble(cordTxt[0]), Double.parseDouble(cordTxt[1]));
            loggerPush("LOADING\t\tGet node cords: "+coords);


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
        alert.showAndWait();
    }
    
    //TODO: ЗДЕСЬ БУДЕТ ВЫВОД ТЕКСТА В ПАНЕЛЬКУ ВНИЗУ ПРОГРАММЫ
    private void loggerPush(String text) {
        //System.out.println(text);
    }

    private Vector<Pair<Node, Color>> revResortStack(Vector<Pair<Node, Color>> stack) {
        Vector<Pair<Node, Color>> result = new Vector<>(stack);
        result.replaceAll(para -> {
            try {
                if (para.getValue().equals(Color.GRAY)) {
                    return new Pair<>(para.getKey(), Color.WHITE);
                }
                if (para.getValue().equals(Color.BLACK)) {
                    return new Pair<>(para.getKey(), Color.GRAY);
                }
            } catch (NullPointerException _) {}
            return para;
        });

        List<Pair<Node, Color>> toSkip = new ArrayList<>();

        for(int i = result.size()-1; i > 0; i--) {
            var temp = result.get(i);
            if(temp.getValue() != null && !toSkip.contains(temp)) {
                int index = i;
                while(result.get(index).getValue()==null || result.get(index).getKey() == result.get(i).getKey()){
                    index--;
                    if(index < 0 || (result.get(index).getKey()!=result.get(i).getKey() && result.get(index).getValue()==null)) {break;}
                }
                index++;
                result.remove(i);
                result.add(index, temp);
                toSkip.add(temp);
                i++;
            }
        }

        return result;
    }
}
