import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import kotlin.jvm.Synchronized;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class Main extends Application {
    private static Graph graph = new Graph();
    private final static Vector<String> titlesVec = new Vector<>();
    private static String title;

    static manualModes manualDraw_Mode = manualModes.NONE;
    static Node selectedNodes = null;

    private static double nodesRadius = 32;
    private static double minNodesDist = nodesRadius*3;
    private static double minNodeToArch = nodesRadius+2;

    private static final AtomicInteger retryCounter = new AtomicInteger(0);

    private File choosenFile = null;

    private static Stage mainStage;
    private static Pane drawSpace;
    private static VBox logger;
    private static VBox structs;

    private static Vector<Pair<Node, Color>> algoStack;
    private static Vector<Pair<Node, Color>> reverseAlgoStack;

    private static Vector<Node> nodesStack = new Vector<>();
    private static Vector<Color> colorsStack = new Vector<>();
    private static Vector<Integer> tinsStack = new Vector<>();
    private static Vector<Integer> tlowsStack = new Vector<>();

    private static final Vector<Arch> bridges = new Vector<>();

    private static AnchorPane algoPanelBlocker;
    private final AtomicReference<Button> algoButtonNext = new AtomicReference<>();
    private final AtomicReference<Button> algoButtonPrev = new AtomicReference<>();

    private final AtomicInteger actualStep = new AtomicInteger(-1);
    private Node actualNode;

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
                node.updateText();
                node.setColor(Color.WHITE);
                node.turnOffHighlight();
            });
            actualStep.set(-1);
            bridges.forEach(arch -> {if(arch!=null) arch.turnOffHighlight();});
            try{
                nodesStack.clear();
                colorsStack.clear();
                tinsStack.clear();
                tlowsStack.clear();
                bridges.clear();
                javafx.scene.Node header = structs.getChildren().getFirst();
                structs.getChildren().clear();
                structs.getChildren().add(header);
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

        logger = new VBox();
        ScrollPane logPanel = new ScrollPane();
        logPanel.setVmax(125);
        logPanel.setVvalue(125);
        logger.maxHeight(logPanel.getVmax());
        logger.setMaxHeight(logPanel.getVmax());
        logger.setPrefHeight(logPanel.getVmax());
        logPanel.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        logPanel.setContent(logger);
        logPanel.vvalueProperty().bind(logger.heightProperty());
        Label logerTitle = new Label("Logs");
        Button clearLogger = new Button("Clear logs");
        clearLogger.setOnAction(_ -> {
            logger.getChildren().clear();
        });
        HBox filler = new HBox();
        HBox loggerHBox = new HBox(logerTitle, filler, clearLogger);
        loggerHBox.setBackground(Background.fill(Color.LIGHTGRAY));
        HBox.setHgrow(filler, Priority.ALWAYS);
        loggerHBox.setPadding(new Insets(5, 10, 5, 10));
        loggerHBox.setAlignment(Pos.BASELINE_CENTER);
        logerTitle.setLabelFor(logPanel);
        VBox loggerBox = new VBox(loggerHBox, logPanel);

        structs = new VBox();
        ScrollPane structsScrollPanel = new ScrollPane();
        logPanel.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        structsScrollPanel.setHmax(235);
        structsScrollPanel.setHvalue(235);
        structs.setPrefWidth(235);
        structs.setMaxWidth(235);
        structs.setAlignment(Pos.BASELINE_CENTER);
        structsScrollPanel.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        structsScrollPanel.hvalueProperty().set(0);
        structsScrollPanel.setContent(structs);
        HBox nodesLabel = new HBox(new Label("Node"));
        nodesLabel.setPrefWidth(35);
        nodesLabel.setAlignment(Pos.BASELINE_CENTER);
        HBox colorsLabel = new HBox(new Label("Color"));
        colorsLabel.setPrefWidth(70);
        colorsLabel.setAlignment(Pos.BASELINE_CENTER);
        HBox tinsLabel = new HBox(new Label("Tin"));
        tinsLabel.setPrefWidth(35);
        tinsLabel.setAlignment(Pos.BASELINE_CENTER);
        HBox lowsLabel = new HBox(new Label("Low"));
        lowsLabel.setPrefWidth(35);
        lowsLabel.setAlignment(Pos.BASELINE_CENTER);
        HBox bridgeLabel = new HBox(new Label("Bridge"));
        bridgeLabel.setPrefWidth(45);
        bridgeLabel.setAlignment(Pos.BASELINE_CENTER);
        structs.getChildren().add(new HBox(nodesLabel, colorsLabel, tinsLabel, lowsLabel, bridgeLabel));

        BorderPane root = new BorderPane();
        root.setCenter(drawSpace);
        root.setRight(rightPanel);
        root.setTop(toolBar);
        root.setBottom(loggerBox);
        root.setLeft(structsScrollPanel);
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

            retryCounter.set(0);

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

            for(int i = 0; i < bridges.size(); i++) {
                KeyFrame keyFrame1 = new KeyFrame(Duration.millis(
                        i*(1000/algorithmSpeed.getValue())+(1000/algorithmSpeed.getValue())), actionEvent2 -> {
                                newNextStep();

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
            newBackStep();
            if(actualStep.get() <= 0) {
                previous.setDisable(true);
            }
        });

        next.setOnAction(actionEvent -> {
            previous.setDisable(false);
            newNextStep();
            if(actualStep.get() >= bridges.size()-1) {
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
                nodesStack = new Vector<>();
                colorsStack = new Vector<>();
                tinsStack = new Vector<>();
                tlowsStack = new Vector<>();

                stepInDepth(graph.getNodes().getFirst(), null);
                graph.getNodes().getFirst().turnOnHighlight();
                overlay.setVisible(false);
                for (int i = 0; i < bridges.size(); i++) {
                    HBox node = new HBox(new Label(nodesStack.get(i) != null ? nodesStack.get(i).toString() : ""));
                    node.setPrefWidth(35);
                    node.setStyle("-fx-border-width: 1pt; -fx-border-color: black");
                    node.setAlignment(Pos.BASELINE_CENTER);
                    HBox color = new HBox(new Label(colorsStack.get(i) != null ? colorsStack.get(i).toString() : ""));
                    color.setPrefWidth(70);
                    color.setAlignment(Pos.BASELINE_CENTER);
                    color.setStyle("-fx-border-width: 1pt; -fx-border-color: black");
                    HBox tin = new HBox(new Label(tinsStack.get(i) != null ? tinsStack.get(i).toString() : ""));
                    tin.setPrefWidth(35);
                    tin.setAlignment(Pos.BASELINE_CENTER);
                    tin.setStyle("-fx-border-width: 1pt; -fx-border-color: black");
                    HBox low = new HBox(new Label(tlowsStack.get(i) != null ? tlowsStack.get(i).toString() : ""));
                    low.setPrefWidth(35);
                    low.setAlignment(Pos.BASELINE_CENTER);
                    low.setStyle("-fx-border-width: 1pt; -fx-border-color: black");
                    HBox bridge = new HBox(new Label(bridges.get(i) != null ? bridges.get(i).toString() : ""));
                    bridge.setPrefWidth(40);
                    bridge.setAlignment(Pos.BASELINE_CENTER);
                    bridge.setStyle("-fx-border-width: 1pt; -fx-border-color: black");
                    HBox temp = new HBox(node, color, tin, low, bridge);
                    structs.getChildren().add(temp);
                }
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

    private void newNextStep() {
        int afterStepNode = -1;
        Color afterStepColor = Color.AQUA;
        int afterStepIn = -1;
        int afterStepLow = -1;
        Arch afterStepBridge = null;
        if(actualStep.get() != -1){
            afterStepNode = actualNode.getNumber();
            afterStepColor = actualNode.getColor();
            afterStepIn = actualNode.getTin();
            afterStepLow = actualNode.getLow();
            afterStepBridge = actualStep.get() < bridges.size() ? bridges.get(actualStep.get()) : bridges.getLast();
        }
        actualStep.getAndIncrement();
        if(actualStep.get() >= bridges.size()) {
            actualStep.set(bridges.size()-1);
            return;
        }
        HBox structsRow = (HBox)structs.getChildren().get(actualStep.get()+1);
        structsRow.setBackground(Background.fill(Color.LIGHTSTEELBLUE));
        Node nextNode = nodesStack.get(actualStep.get());
        Object nextTin = tinsStack.get(actualStep.get());
        Object nextLow = tlowsStack.get(actualStep.get());
        Color nextColor = colorsStack.get(actualStep.get());
        Arch nextBridge = bridges.get(actualStep.get());

        if(nextNode != null) {
            if(nextNode != actualNode) {
                try{
                    actualNode.turnOffHighlight();
                } catch (NullPointerException _){}
                nextNode.turnOnHighlight();
                actualNode = nextNode;
            }
        }

        actualNode.setTin(nextTin != null ? (int)nextTin : actualNode.getTin());
        actualNode.setLow(nextLow != null ? (int)nextLow : actualNode.getLow());
        actualNode.setColor(nextColor != null ? nextColor : actualNode.getColor());
        if(afterStepNode == actualNode.getNumber() && afterStepColor == actualNode.getColor() &&
                afterStepIn == actualNode.getTin() && afterStepLow == actualNode.getLow() && afterStepBridge == nextBridge) {
            actualStep.getAndDecrement();
            nodesStack.remove(actualStep.get()+1);
            colorsStack.remove(actualStep.get()+1);
            tinsStack.remove(actualStep.get()+1);
            tlowsStack.remove(actualStep.get()+1);
            bridges.remove(actualStep.get()+1);
            structs.getChildren().remove(actualStep.get()+2);
            newNextStep();
            return;
        }
        actualNode.updateText();
        loggerPush(
                "[Next step] Step="+actualStep.get()+
                        ", node="+actualNode.getNumber()+
                        ", color="+actualNode.getColor()+
                        ", tin="+actualNode.getTin()+
                        ", low="+actualNode.getLow()
        );
        if (nextBridge != null) {
            nextBridge.turnOnHighlight();
            loggerPush("Bridge founded! Its - edge between "+nextBridge.getTransitNodes()[0].getNumber()+
                    " and "+nextBridge.getTransitNodes()[1].getNumber());
        }
    }

//TODO заменить авте на бефо, опечатка
    private void newBackStep() {
        HBox structsRow = (HBox)structs.getChildren().get(actualStep.get()+1);
        structsRow.setBackground(Background.fill(Color.WHITE));

        actualStep.getAndDecrement();
        if(actualStep.get() < 0) {
            actualStep.set(0);
            actualNode.turnOffHighlight();
            return;
        }
        Node nextNode = nodesStack.get(actualStep.get());

        if(nextNode==null && actualStep.get() > 0){
            int nearbyNodeIndex = actualStep.get()-1;
            nextNode = nodesStack.get(nearbyNodeIndex);
            while (nextNode == null) {
                nearbyNodeIndex--;
                if(nearbyNodeIndex <= 0) break;
                nextNode = nodesStack.get(nearbyNodeIndex);
            }
        }
        Object nextTin = tinsStack.get(actualStep.get());
//        if(nextTin == null) {
//            nextTin = getPrevParam(tinsStack, actualStep.get(), nextNode != null ? nextNode : actualNode);
//        }
        Object nextLow = tlowsStack.get(actualStep.get());
//        if(nextLow == null) {
//            nextLow = getPrevParam(tlowsStack, actualStep.get(), nextNode != null ? nextNode : actualNode);
//        }
        int nearbyColorIndex = actualStep.get();
        Color nextColor = colorsStack.get(nearbyColorIndex);
        if(nextColor == null){
            nextColor = getPrevParam(colorsStack, actualStep.get(), nextNode != null ? nextNode : actualNode);

        }
        if(nextColor == Color.BLACK) nextColor = Color.GRAY;
        else if(nextColor == Color.GRAY) nextColor = Color.WHITE;
        Arch nextBridge = bridges.get(actualStep.get());

        if(nextNode != null) {
            if(nextNode != actualNode) {
                try{
                    actualNode.turnOffHighlight();
                } catch (NullPointerException _){}
                nextNode.turnOnHighlight();
                actualNode = nextNode;
            }
        }
        actualNode.setTin(nextTin != null ? (int)nextTin : actualNode.getTin());
        actualNode.setLow(nextLow != null ? (int)nextLow : actualNode.getLow());
        actualNode.setColor(nextColor != null ? nextColor : actualNode.getColor());

        actualNode.updateText();
        loggerPush(
                "[Back step] Step="+actualStep.get()+
                        ", node="+actualNode.getNumber()+
                        ", color="+actualNode.getColor()+
                        ", tin="+actualNode.getTin()+
                        ", low="+actualNode.getLow()
        );
        if (nextBridge != null) {
            nextBridge.turnOffHighlight();
            loggerPush("Bridge losted... It was edge between "+nextBridge.getTransitNodes()[0].getNumber()+
                    " and "+nextBridge.getTransitNodes()[1].getNumber()+'.');
        }
    }

    private <T> T getPrevParam(Vector<T> stack, int startStep, Node targetNode) {
        loggerPush("[FINDprevious]\t\tMethod was called.");
        int tempStep = startStep;
        loggerPush("[FINDprevious]\t\tInit target node & tempStep. Try found last entry this node");
        while(nodesStack.get(tempStep) != targetNode) {
            loggerPush("[FINDprevious]\t\tGo to back.");
            tempStep--;
            if(tempStep <= 0) {
                loggerPush("[FINDprevious]\t\tEnd stack. Return.");
                break;
            }
        }
        loggerPush("[FINDprevious]\tTry found parameter for this node.");
        T parameter = null;
        while(stack.get(tempStep) == null) {
            loggerPush("[FINDprevious]\t\tGo forward.");
            tempStep++;
            parameter = stack.get(tempStep) != null ? stack.get(tempStep) : parameter;
            if(tempStep >= nodesStack.size()) {
                loggerPush("[FINDprevious]\t\tEnd stack.");
                tempStep = nodesStack.size()-1;
                break;
            }
            if(nodesStack.get(tempStep) != null && nodesStack.get(tempStep) != actualNode && parameter == null) {
                loggerPush("[FINDprevious]\t\tFinded another node, go deeper.");
                return getPrevParam(stack, tempStep, targetNode);
            }
        }
        loggerPush("[FINDprevious]\t\tGot it.");
        return stack.get(tempStep);
    }

    //TODO: добавить отображение структур данных. Короче говоря - отобразить метки времени у узлов
    private void stepInDepth(final Node curNode, final Node parent) {
        nodesStack.add(curNode);
        nodesStack.add(null);

        colorsStack.add(null);
        colorsStack.add(Color.GRAY);

        curNode.setLow(timer);
        tlowsStack.add(-1);
        tlowsStack.add(timer);

        curNode.setTin(timer);
        tinsStack.add(-1);
        tinsStack.add(timer);

        timer++;

        bridges.add(null);
        bridges.add(null);

        Vector<Node> nodes = graph.getAttaches(curNode);
        for(Node to : nodes) {
            if(to.equals(parent)) continue;
            if(to.getTin() == -1) {
                stepInDepth(to, curNode);

                int minlow = Integer.min(curNode.getLow(), to.getLow());
                tlowsStack.add(minlow);
                tinsStack.add(null);
                colorsStack.add(null);
                nodesStack.add(null);
                bridges.add(null);
                curNode.setLow(minlow);

                if(to.getLow() > curNode.getTin()) {
                    bridges.add(graph.findArch(curNode, to));
                    tlowsStack.add(null);
                    tinsStack.add(null);
                    colorsStack.add(null);
                    nodesStack.add(null);
                }
            }
            else {
                int minlow = Integer.min(curNode.getLow(), to.getTin());
                curNode.setLow(minlow);
                tlowsStack.add(minlow);
                tinsStack.add(null);
                colorsStack.add(null);
                nodesStack.add(null);
                bridges.add(null);
            }
            int lastNodeId = nodesStack.size()-1;
            Node lastNode = nodesStack.get(lastNodeId);
            while(lastNode == null) {
                lastNodeId--;
                lastNode = nodesStack.get(lastNodeId);
            }
            if(!lastNode.equals(curNode)) {
                tlowsStack.add(null);
                tinsStack.add(null);
                colorsStack.add(null);
                nodesStack.add(curNode);
                bridges.add(null);
            }
        }
        tlowsStack.add(null);
        tlowsStack.add(null);
        tinsStack.add(null);
        tinsStack.add(null);
        colorsStack.add(null);
        colorsStack.add(Color.BLACK);
        nodesStack.add(curNode);
        nodesStack.add(null);
        bridges.add(null);
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

        MenuItem minAngleNodes = new MenuItem("Min. angle btw nodes");
        minAngleNodes.setOnAction(actionEvent -> insertminAngleBTWNodes());

        MenuItem nodesRadius = new MenuItem("Nodes radius");
        nodesRadius.setOnAction(actionEvent -> insertRadiusNodes());

        optionsMenu.getItems().addAll(minDistNodes, minAngleNodes, nodesRadius);

        Button downscale = new Button("-");
        downscale.setPrefWidth(25);
        downscale.setOnAction(_ -> {
            drawSpace.setScaleX(drawSpace.getScaleX()*0.9);
            drawSpace.setScaleY(drawSpace.getScaleY()*0.9);
        });
        Button upscale = new Button("+");
        upscale.setPrefWidth(25);
        upscale.setOnAction(_ -> {
            drawSpace.setScaleX(drawSpace.getScaleX()*1.1);
            drawSpace.setScaleY(drawSpace.getScaleY()*1.1);
        });

        Button resetscale = new Button("Reset scale");
        resetscale.setOnAction(_ -> {
            drawSpace.setScaleX(1);
            drawSpace.setScaleY(1);
        });

        menuBar.getMenus().addAll(fileMenu, optionsMenu);
        toolBar.getItems().addAll(new Label("Change drawing panel scale: "), downscale, upscale, resetscale);
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

    private void insertminAngleBTWNodes() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(minNodeToArch));
        dialog.setTitle("Insert minimal angle");
        dialog.setHeaderText("Please, insert new minimal angle between nodes.");
        dialog.setContentText("Minimal angle:");
        dialog.showAndWait().ifPresent(newValue -> {
            double val = Double.parseDouble(newValue);
            if(val >= 0 || val <= 360) {
                minNodeToArch = val;
            } else {
                wrongInputAlert("Input value must be only greater or equals than 0 and lower or equals 360!");
            }
        });
    }

    private Node placeGraphNode(Coords coords, int number) {
        if(coords.getX() <= nodesRadius || coords.getX() >= drawSpace.getWidth()-nodesRadius || coords.getY() <= nodesRadius || coords.getY() >= drawSpace.getHeight()-nodesRadius) {
            loggerPush("ERROR\t\tToo near to border drawing panel.");
            return null;
        }

        Circle circleTmp = new Circle(coords.getX(), coords.getY(), nodesRadius);

        AtomicBoolean collision = new AtomicBoolean(false);
        graph.getNodes().forEach(node -> {
            if(Coords.minus(node.getPos(), coords) < minNodesDist) {
                collision.set(true);
            }
            graph.getAttaches(node).forEach(attaNodes -> {
                double distFromCoordsToArch = getDist(node.getPos(), attaNodes.getPos(), coords);
                if(distFromCoordsToArch < minNodeToArch) collision.set(true);
            });
        });
        if (collision.get()) {
            loggerPush("ERROR\t\tCannot place node at here. Too close to another arch or node.");
            return null;
        }

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
                    graph.deleteNode(node, drawSpace);
                    break;
                }
            }
        });
        return node;
    }


    private double getDist(Coords linePtA, Coords linePtB, Coords point){
        double px = point.getX(), py = point.getY();
        double ax = linePtA.getX(), ay = linePtA.getY();
        double bx = linePtB.getX(), by = linePtB.getY();

        double dx = bx - ax;
        double dy = by - ay;

        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double closestX = ax + t * dx;
        double closestY = ay + t * dy;

        return Math.hypot(px - closestX, py - closestY);
    }

    private boolean doAttachment(Node firstNode, Node secondNode) {
        selectedNodes = null;

        loggerPush("DO_ATTACH\t\tDeselection nodes.");
        firstNode.deSelect();
        secondNode.deSelect();

        Vector<Node> nodes = new Vector<>(graph.getNodes());
        nodes.remove(firstNode);
        nodes.remove(secondNode);

        AtomicBoolean collision = new AtomicBoolean(false);
        nodes.forEach(node -> {
            double distFromCoordsToArch = getDist(firstNode.getPos(), secondNode.getPos(), node.getPos());
            if(distFromCoordsToArch < minNodeToArch) collision.set(true);
        });
        if (collision.get()) {
            loggerPush("ERROR\t\tCannot attach - on the line between this nodes already has been placed another node");
            return false;
        }

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
        if(retryCounter.get() >= 50) {
            loggerPush("ERROR -- PROGRAM CANNOT BUILD GRAPH - TOO DEEP RECURSIVE STACK");
        }

        loggerPush("RANDOM_GEN\t\tStart generating random graph.");

        reset();

        Random random = new Random();
        for (int countNode = 0; countNode < nodesAmount; countNode++) {
            Node nodeTmp = null;
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
            int tryingCounter = 0;
            while(!tryAttach) {
                if(tryingCounter > archesAmount) {
                    retryCounter.getAndIncrement();
                    randomGraph(nodesAmount, archesAmount);
                    return;
                }
                tryAttach = doAttachment(
                        nodes.get(random.nextInt(nodesAmount)),
                        nodes.get(random.nextInt(nodesAmount))
                );
                tryingCounter++;
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
        System.out.println(text);
        Text log = new Text(text);
        log.setTextAlignment(TextAlignment.LEFT);
        logger.getChildren().add(log);
    }
}
