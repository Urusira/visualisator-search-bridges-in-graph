import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
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

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class Main extends Application {
    public static Locale currentLocale = new Locale("en");
    public static ResourceBundle bundle = ResourceBundle.getBundle("lang.lang", currentLocale);

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
        //title = "Поиск мостов";

        launch(args);
    }

    @Override
    public void start(Stage stage) {
        mainStage = stage;

        BorderPane root = rootInit();

        titleUpdate();
        stage.setMinWidth(720);
        stage.setMinHeight(440);
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
        loggerPush(bundle.getString("LOG_startReset"));
        selectedNodes = null;
        graph = new Graph();
        loggerPush(bundle.getString("LOG_graphCleared"));
        drawSpace.getChildren().clear();
        loggerPush(bundle.getString("LOG_panelCleared"));
    }

    private BorderPane rootInit() {
        drawSpace = new Pane();
        drawSpace.setId("drawSpace");

        Tab graphBuildTab = new Tab(bundle.getString("EL_tabBuilder"), graphBuilderInit());
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

        Tab algorythmTab = new Tab(bundle.getString("EL_tabAlgo"), algoPanelInit());
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
        Label logerTitle = new Label(bundle.getString("EL_logsTitle"));
        Button clearLogger = new Button(bundle.getString("EL_clearLogs"));
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
        HBox nodesLabel = new HBox(new Label(bundle.getString("EL_structsNode")));
        nodesLabel.setPrefWidth(35);
        nodesLabel.setAlignment(Pos.BASELINE_CENTER);
        HBox colorsLabel = new HBox(new Label(bundle.getString("EL_structsColor")));
        colorsLabel.setPrefWidth(70);
        colorsLabel.setAlignment(Pos.BASELINE_CENTER);
        HBox tinsLabel = new HBox(new Label(bundle.getString("EL_structsTin")));
        tinsLabel.setPrefWidth(35);
        tinsLabel.setAlignment(Pos.BASELINE_CENTER);
        HBox lowsLabel = new HBox(new Label(bundle.getString("EL_structsLow")));
        lowsLabel.setPrefWidth(35);
        lowsLabel.setAlignment(Pos.BASELINE_CENTER);
        HBox bridgeLabel = new HBox(new Label(bundle.getString("EL_structsBridge")));
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
        Label tbLabel = new Label(bundle.getString("EL_bderManualTitle"));
        tbLabel.setId("rightPanelLabel");

        Button nodesMode = new Button(bundle.getString("EL_bderBtnNod"));
        nodesMode.setOnAction(actionEvent -> {
            manualDraw_Mode = manualModes.NODES;
        });

        Button archesMode = new Button(bundle.getString("EL_bderBtnArc"));
        archesMode.setOnAction(actionEvent -> {
            manualDraw_Mode = manualModes.ARCHES;
        });

        Button delMode = new Button(bundle.getString("EL_bderBtnDel"));
        delMode.setOnAction(actionEvent -> {
            manualDraw_Mode = manualModes.DELETE;
        });

        Button resetButton = new Button(bundle.getString("EL_bderBtnRes"));
        resetButton.setOnAction(actionEvent -> {
            reset();
        });

        Label randomLabel = new Label(bundle.getString("EL_bderRandomTitle"));
        TextField nodesAmount = new TextField();
        TextField archesAmount = new TextField();

        nodesAmount.setPromptText(bundle.getString("EL_bderPmtNode"));
        archesAmount.setPromptText(bundle.getString("EL_bderPmtArch"));

        HBox randomParams = new HBox(nodesAmount, archesAmount);

        Button randomGraph = new Button(bundle.getString("EL_bderBtnGen"));
        randomGraph.setOnAction(actionEvent -> {
            int nodAmoInt;
            int archAmoInt;

            retryCounter.set(0);

            try {
                nodAmoInt = Integer.parseInt(nodesAmount.getText());
            } catch (NumberFormatException e) {
                wrongInputAlert(bundle.getString("ALERT_incorrIn"));
                return;
            }

            int maxArches = ((nodAmoInt-1)*nodAmoInt)/2;

            if(archesAmount.getText().isEmpty()) {archAmoInt = maxArches;}
            else {
                try {
                    archAmoInt = Integer.parseInt(archesAmount.getText());
                    if(archAmoInt > maxArches) {archAmoInt = maxArches;}
                } catch (NumberFormatException e) {
                    wrongInputAlert(bundle.getString("ALERT_incorrIn2"));
                    return;
                }
            }
            if (nodAmoInt >= 0 && archAmoInt >= 0) {
                randomGraph(nodAmoInt, archAmoInt);
            }
            else {
                wrongInputAlert(bundle.getString("ALERT_negateError"));
            }
        });

        HBox modeSelectBox = new HBox(nodesMode, archesMode, delMode);
        modeSelectBox.setId("modeSelectBox");

        VBox graphBuildTab = new VBox(tbLabel, modeSelectBox, resetButton , randomLabel, randomParams, randomGraph);
        graphBuildTab.setId("graphBuildTab");
        return graphBuildTab;
    }

    private StackPane algoPanelInit() {
        Label algSpeedLabel = new Label(bundle.getString("EL_algoSpd"));
        Slider algorithmSpeed = new Slider(1d, 10, 1);
        algorithmSpeed.setShowTickLabels(true);
        algorithmSpeed.setShowTickMarks(true);

        Button startRun = new Button(bundle.getString("EL_algoBtnStartVis"));
        startRun.setId("startAlgo");

        Button previous = new Button(bundle.getString("EL_algoBtnBack"));
        previous.setDisable(true);
        algoButtonPrev.set(previous);

        Button next = new Button(bundle.getString("EL_algoBtnNext"));
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
        manualSteps.setAlignment(Pos.BASELINE_CENTER);
        manualSteps.setSpacing(5);

        AnchorPane overlay = new AnchorPane();
        overlay.setId("algorithmBlocker");
        overlay.setPickOnBounds(true);

        Button startButton = new Button(bundle.getString("EL_algoStart"));
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
                wrongInputAlert(bundle.getString("ALERT_needGraph"));
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
                bundle.getString("LOG_nextStep")+"="+actualStep.get()+
                        ", "+bundle.getString("LOG_stepNode")+"="+actualNode.getNumber()+
                        ", "+bundle.getString("LOG_stepColor")+"="+actualNode.getColor()+
                        ", "+bundle.getString("LOG_stepTin")+"="+actualNode.getTin()+
                        ", "+bundle.getString("LOG_stepLow")+"="+actualNode.getLow()
        );
        if (nextBridge != null) {
            nextBridge.turnOnHighlight();
            loggerPush(bundle.getString("LOG_bridgeFound")+" "+nextBridge.getTransitNodes()[0].getNumber()+
                    "-"+nextBridge.getTransitNodes()[1].getNumber());
        }
    }

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
//WIP
//        if(nextTin == null) {
//            nextTin = getPrevParam(tinsStack, actualStep.get(), nextNode != null ? nextNode : actualNode);
//        }
        Object nextLow = tlowsStack.get(actualStep.get());
//WIP
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
                bundle.getString("LOG_backStep")+"="+actualStep.get()+
                        ", "+bundle.getString("LOG_stepNode")+"="+actualNode.getNumber()+
                        ", "+bundle.getString("LOG_stepColor")+"="+actualNode.getColor()+
                        ", "+bundle.getString("LOG_stepTin")+"="+actualNode.getTin()+
                        ", "+bundle.getString("LOG_stepLow")+"="+actualNode.getLow()
        );
        if (nextBridge != null) {
            nextBridge.turnOffHighlight();
            loggerPush(bundle.getString("LOG_bridgeLost")+" "+nextBridge.getTransitNodes()[0].getNumber()+
                    "-"+nextBridge.getTransitNodes()[1].getNumber()+'.');
        }
    }

    private <T> T getPrevParam(Vector<T> stack, int startStep, Node targetNode) {
        int tempStep = startStep;
        while(nodesStack.get(tempStep) != targetNode) {
            tempStep--;
            if(tempStep <= 0) {
                break;
            }
        }
        T parameter = null;
        while(stack.get(tempStep) == null) {
            tempStep++;
            parameter = stack.get(tempStep) != null ? stack.get(tempStep) : parameter;
            if(tempStep >= nodesStack.size()) {
                tempStep = nodesStack.size()-1;
                break;
            }
            if(nodesStack.get(tempStep) != null && nodesStack.get(tempStep) != actualNode && parameter == null) {
                return getPrevParam(stack, tempStep, targetNode);
            }
        }
        loggerPush(bundle.getString("LOG_restoreParam"));
        return stack.get(tempStep);
    }

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

        Menu fileMenu = new Menu(bundle.getString("EL_menuFile"));

        MenuItem saveItem = new MenuItem(bundle.getString("EL_menuFileSave"));
        saveItem.setOnAction(actionEvent -> {
            try {
                saveGraph(false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        MenuItem saveAsItem = new MenuItem(bundle.getString("EL_menuFileSaveAs"));
        saveAsItem.setOnAction(actionEvent -> {
            try {
                saveGraph(true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        MenuItem loadItem = new MenuItem(bundle.getString("EL_menuFileLoad"));
        loadItem.setOnAction(actionEvent -> {
            try {
                loadGraph();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        fileMenu.getItems().addAll(saveItem, saveAsItem, loadItem);

        Menu optionsMenu = new Menu(bundle.getString("EL_menuOpts"));

        MenuItem minDistNodes = new MenuItem(bundle.getString("EL_menuOptsNNDist"));
        minDistNodes.setOnAction(actionEvent -> insertMinDist());

        MenuItem minNodeArchDist = new MenuItem(bundle.getString("EL_menuOptsNADist"));
        minNodeArchDist.setOnAction(actionEvent -> insertminMinDistToArch());

        MenuItem nodesRadius = new MenuItem(bundle.getString("EL_menuOptsNodRad"));
        nodesRadius.setOnAction(actionEvent -> insertRadiusNodes());

        optionsMenu.getItems().addAll(minDistNodes, minNodeArchDist, nodesRadius);

        Button downscale = new Button(bundle.getString("EL_menuDownscale"));
        downscale.setPrefWidth(25);
        downscale.setOnAction(_ -> {
            drawSpace.setScaleX(drawSpace.getScaleX()*0.9);
            drawSpace.setScaleY(drawSpace.getScaleY()*0.9);
        });
        Button upscale = new Button(bundle.getString("EL_menuUppscale"));
        upscale.setPrefWidth(25);
        upscale.setOnAction(_ -> {
            drawSpace.setScaleX(drawSpace.getScaleX()*1.1);
            drawSpace.setScaleY(drawSpace.getScaleY()*1.1);
        });

        Button resetscale = new Button(bundle.getString("EL_menuResetScale"));
        resetscale.setOnAction(_ -> {
            drawSpace.setScaleX(1);
            drawSpace.setScaleY(1);
        });

        Button changeLang = new Button(bundle.getString("EL_changeLang"));
        changeLang.setOnAction(_ -> {
            changeLang();
        });

        menuBar.getMenus().addAll(fileMenu, optionsMenu);
        toolBar.getItems().addAll(
                new Label(bundle.getString("EL_menuScalesLabel")+": "),
                downscale, upscale, resetscale,
                new Label(bundle.getString("EL_langLabel")+": "),
                changeLang,
                new Label(bundle.getString("EL_langAttention"))
        );
        return toolBar;
    }

    private void insertMinDist() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(minNodesDist));
        dialog.setTitle(bundle.getString("DIA_titleMinDist"));
        dialog.setHeaderText(bundle.getString("DIA_headNNDist"));
        dialog.setContentText(bundle.getString("DIA_contentNNDist")+":");
        dialog.showAndWait().ifPresent(newValue -> {
            if(Double.parseDouble(newValue) > 0) {
                minNodesDist = Double.parseDouble(newValue);
            } else {
                wrongInputAlert(bundle.getString("ALERT_negateError"));
            }
        });
    }

    private void insertRadiusNodes() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(nodesRadius));
        dialog.setTitle(bundle.getString("DIA_titleRadius"));
        dialog.setHeaderText(bundle.getString("DIA_headRadius"));
        dialog.setContentText(bundle.getString("DIA_contentRadius")+":");
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
                wrongInputAlert(bundle.getString("ALERT_greaterThan7"));
            }
        });
    }

    private void insertminMinDistToArch() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(minNodeToArch));
        dialog.setTitle(bundle.getString("DIA_titleMinDist"));
        dialog.setHeaderText(bundle.getString("DIA_headNADist"));
        dialog.setContentText(bundle.getString("DIA_contentNADist")+":");
        dialog.showAndWait().ifPresent(newValue -> {
            double val = Double.parseDouble(newValue);
            if(val > 0) {
                minNodeToArch = val;
            } else {
                wrongInputAlert(bundle.getString("ALERT_negateError"));
            }
        });
    }

    private Node placeGraphNode(Coords coords, int number) {
        if(coords.getX() <= nodesRadius || coords.getX() >= drawSpace.getWidth()-nodesRadius || coords.getY() <= nodesRadius || coords.getY() >= drawSpace.getHeight()-nodesRadius) {
            loggerPush(bundle.getString("LOG_borderClose"));
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
            loggerPush(bundle.getString("LOG_collision"));
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
                    loggerPush(bundle.getString("LOG_clickInside")+" " + node.getNumber() + ".");
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

        loggerPush(bundle.getString("LOG_deselectNodes"));
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
            loggerPush(bundle.getString("LOG_collisionTrace"));
            return false;
        }

        loggerPush(bundle.getString("LOG_tryingAttach"));
        boolean tryAttach = graph.addAttach(firstNode, secondNode);
        if(!tryAttach) {
            loggerPush(bundle.getString("LOG_cannotAttach"));
            return false;
        }

        loggerPush(bundle.getString("LOG_makingAttach"));
        Coords fstCenter = firstNode.getPos();
        Coords sndCenter = secondNode.getPos();

        Line lineTmp = new Line(fstCenter.getX(), fstCenter.getY(), sndCenter.getX(), sndCenter.getY());

        loggerPush(bundle.getString("LOG_creatingArch"));
        Arch arch = new Arch(firstNode, secondNode, lineTmp);

        loggerPush(bundle.getString("LOG_addAttachInNodes"));
        firstNode.addAttachment(arch);
        secondNode.addAttachment(arch);

        loggerPush(bundle.getString("LOG_createArchHandler"));
        lineTmp.setOnMouseClicked(mouseEvent -> {
            if(manualDraw_Mode == manualModes.DELETE) {
                graph.deleteArch(firstNode, secondNode, arch, drawSpace);
            }
        });

        loggerPush(bundle.getString("LOG_drawindLine"));
        drawSpace.getChildren().addFirst(lineTmp);

        loggerPush(bundle.getString("LOG_succesAttach"));
        return true;
    }

    private void randomGraph(final int nodesAmount, int archesAmount) {
        if(retryCounter.get() >= 50) {
            loggerPush(bundle.getString("LOG_cannotGenerate"));
            return;
        }

        loggerPush(bundle.getString("LOG_startGenerate"));

        reset();

        Random random = new Random();
        for (int countNode = 0; countNode < nodesAmount; countNode++) {
            Node nodeTmp = null;
            int tryingCounter = 0;
            while(nodeTmp == null) {
                if(tryingCounter >= 50) {
                    loggerPush(bundle.getString("LOG_cannotGenerate"));
                    break;
                }
                Coords coords = new Coords(
                        random.nextDouble(drawSpace.getWidth()),
                        random.nextDouble(drawSpace.getHeight())
                );
                nodeTmp = placeGraphNode(coords, countNode);
                tryingCounter++;
            }
        }

        Vector<Node> nodes = graph.getNodes();
        retryCounter.set(0);

        for(int countArches = 0; countArches < archesAmount; countArches++) {
            boolean tryAttach = false;
            int tryingCounter = 0;
            while(!tryAttach) {
                if(tryingCounter > 50) {
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
        loggerPush(bundle.getString("LOG_startSave"));

        if(asNew) {
            FileChooser fc = new FileChooser();
            fc.setTitle(bundle.getString("DIA_titleSave"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(bundle.getString("DIA_formatsFile"), "*.graph"));
            fc.setInitialFileName(bundle.getString("DIA_fileName") + "-" + now.getHour() + now.getMinute() + now.getSecond() + now.getNano());
            choosenFile = fc.showSaveDialog(mainStage);
        }

        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(choosenFile));
        } catch (NullPointerException e) {
            loggerPush(bundle.getString("LOG_haventSavePath"));
            if(!asNew) {
                loggerPush(bundle.getString("LOG_newPath"));
                saveGraph(true);
            }
            return;
        }

        loggerPush(bundle.getString("LOG_savingInit"));
        writer.write("FILE_TYPE-GRAPH");
        writer.newLine();
        writer.write(String.valueOf(now));
        writer.newLine();

        writer.write(graph.toString());
        writer.newLine();
        writer.close();
        titleUpdate();
        loggerPush(bundle.getString("LOG_savingDone"));
    }

    private void loadGraph() throws FileNotFoundException {
        algoPanelBlocker.setVisible(true);

        loggerPush(bundle.getString("LOG_statLoad"));
        Scanner sc;

        FileChooser fc = new FileChooser();
        fc.setTitle(bundle.getString("DIA_titleLoad"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(bundle.getString("DIA_formatsFile"), "*.graph", "*.txt"));
        try {
            fc.setInitialDirectory(choosenFile.getParentFile());
        } catch (NullPointerException e) {
            loggerPush(bundle.getString("LOG_bufferEmpty"));
        }
        choosenFile = fc.showOpenDialog(mainStage);
        try {
            sc = new Scanner(choosenFile);
            loggerPush(bundle.getString("LOG_readFile"));
        } catch (FileNotFoundException e) {
            loggerPush(bundle.getString("LOG_loadNotFound"));
            return;
        } catch (NullPointerException e) {
            loggerPush(bundle.getString("LOG_fileNotChoosed"));
            return;
        }

        String fileTypeCheck = sc.nextLine();
        if(!Objects.equals(fileTypeCheck, "FILE_TYPE-GRAPH")) {
            loggerPush(bundle.getString("LOG_wrongFile"));
            return;
        }

        reset();
        sc.nextLine();

        while(sc.hasNextLine()) {
            String loadedLine = sc.nextLine();
            loggerPush(bundle.getString("LOG_readNextLine"));

            String[] cordTxt = loadedLine.substring(loadedLine.lastIndexOf("["), loadedLine.lastIndexOf("]")).replaceAll("[^\\d.\\s]", "").split(" ");

            int number = Integer.parseInt(loadedLine.substring(0, loadedLine.indexOf(" ")));
            loggerPush(bundle.getString("LOG_getNodeNum")+" - "+number);

            Coords coords = new Coords(Double.parseDouble(cordTxt[0]), Double.parseDouble(cordTxt[1]));
            loggerPush(bundle.getString("LOG_getNodeCords")+": "+coords);


            Node nodeTemp = placeGraphNode(coords, number);
            loggerPush(bundle.getString("LOG_nodeCreated1")+" "+nodeTemp.getNumber()+" "+bundle.getString("LOG_nodeCreated2"));

            String[] attachmentsTxt = loadedLine.substring(loadedLine.indexOf("["), loadedLine.indexOf("]")).replaceAll("[^\\d.\\s]", "").split(" ");
            loggerPush(bundle.getString("LOG_attachments")+" "+nodeTemp.getNumber()+":"+ Arrays.toString(attachmentsTxt));

            for (String attachNode : attachmentsTxt) {
                loggerPush(bundle.getString("LOG_checkAttaches")+" "+nodeTemp.getNumber()+"...");

                if (attachNode != null && !attachNode.isEmpty()) {
                    Node secondNode = graph.findWithNum(Integer.parseInt(attachNode));
                    if (secondNode != null) {
                        loggerPush(bundle.getString("LOG_lookingFor")+" " + attachNode);
                        doAttachment(nodeTemp, secondNode);
                        loggerPush(bundle.getString("LOG_loadSuccesAttach")+" " + nodeTemp.getNumber() + "-" + attachNode);
                    }
                    else {
                        loggerPush(bundle.getString("LOG_attachAlrdCreated"));
                    }
                }
                else {
                    loggerPush(bundle.getString("LOG_haventAttaches"));
                }
            }
        }
        titleUpdate();
    }

    private void wrongInputAlert(String desc) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(bundle.getString("ALERT_title"));
        alert.setHeaderText(null);
        alert.setContentText(desc);
        alert.showAndWait();
    }

    private void loggerPush(String text) {
        System.out.println(text);
        Text log = new Text(text);
        log.setTextAlignment(TextAlignment.LEFT);
        logger.getChildren().add(log);
        if(logger.getChildren().size()>2000) logger.getChildren().removeFirst();
    }

    public void changeLang() {
        if(currentLocale.getLanguage() == "en") {
            currentLocale = new Locale("ru");
        } else {
            currentLocale = new Locale("en");
        }

        bundle = ResourceBundle.getBundle("lang.lang", currentLocale);

        reset();

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

        mainStage.close();
        Platform.runLater(() -> new Main().start(new Stage()));
    }
}
