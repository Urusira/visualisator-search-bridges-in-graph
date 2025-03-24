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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;


public class Main extends Application {

    private static Vector<String> titlesVec = new Vector<>();
    private static String title;
    private static Vector<Node> nodes = new Vector<>();
    private static Vector<Arch> arches = new Vector<>();
    static modes actualMode = modes.NONE;
    static Node selectedNodes = null;

    public static void main(String[] args) throws FileNotFoundException {
        Scanner sc = new Scanner(new File("titles.txt"));
        while (sc.hasNextLine()) {
            titlesVec.add(sc.nextLine());
        }

        sc.close();

        Random rd = new Random();
        title = titlesVec.get(rd.nextInt(titlesVec.capacity()-1));

        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        BorderPane root = rootInit();

        stage.setTitle(title);
        stage.setWidth(720);
        stage.setHeight(440);

        root.getStylesheets().add("main/resources/main.css");

        stage.setScene(new Scene(root));
        stage.show();
    }

    private static BorderPane rootInit() {
        VBox toolBar = toolBarInit();

        Pane drawSpace = new Pane();
        drawSpace.setId("drawSpace");

        drawSpace.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            double clX = mouseEvent.getX();
            double clY = mouseEvent.getY();

            System.out.println("Mouse click X: "+clX+"; Mouse click X: "+clY+"; Actual mode: "+actualMode.name());

            switch (actualMode) {
                case NODES: {
                    placeGraphNode(clX, clY, drawSpace);
                    break;
                }
                case ARCHES: {
                    for(var node : nodes) {
                        if(node.getFigure() instanceof Circle circle) {
                            double circCenterX = circle.getCenterX();
                            double circCenterY = circle.getCenterY();

                            double evcliDist = Math.sqrt(Math.pow(clX-circCenterX, 2)+Math.pow(clY-circCenterY, 2));

                            if(evcliDist <= circle.getRadius()) {
                                node.turnHighlight();
                                System.out.println("Clicked inside circle.");
                                if(selectedNodes == null) {
                                    selectedNodes = node;
                                }
                                else {
                                    doAttachment(selectedNodes, node, drawSpace);
                                    selectedNodes = null;
                                }
                            }
                        }
                    }
                    break;
                }
            }
        });

        BorderPane root = new BorderPane();
        root.setCenter(drawSpace);
        root.setRight(toolBar);
        root.setId("root");

        return root;
    }

    private static VBox toolBarInit() {
        Label tbLabel = new Label("Toolbar");
        tbLabel.setId("toolBarLabel");

        Button changeModeButton = new Button("Установка\nузлов");
        changeModeButton.setOnAction(actionEvent -> {
            changeMode();
            switch (actualMode) {
                case NODES -> changeModeButton.setText("Соединение\nузлов");
                case ARCHES -> changeModeButton.setText("Установка\nузлов");
            }
        });

        VBox toolBar = new VBox(tbLabel, changeModeButton);
        toolBar.setId("toolBar");
        return toolBar;
    }

    public static void changeMode() {
        switch (actualMode) {
            case NONE, ARCHES -> actualMode = modes.NODES;
            case NODES -> actualMode = modes.ARCHES;
        }
    }

    private static void placeGraphNode(double xCoord, double yCoord, Pane drowSpace) {
        Circle circleTmp = new Circle(xCoord, yCoord, 30);
        drowSpace.getChildren().add(circleTmp);
        nodes.add(new Node(new Coords(xCoord, yCoord), circleTmp));
    }

    private static void doAttachment(Node firstNode, Node secondNode, Pane drowSpace) {
        Coords firstPoint = firstNode.getPos();
        Coords secondPoint = secondNode.getPos();

        Line lineTmp = new Line(
                firstPoint.getX(),
                firstPoint.getY(),
                secondPoint.getX(),
                secondPoint.getY()
        );

        drowSpace.getChildren().add(lineTmp);

        arches.add(new Arch(firstNode, secondNode, lineTmp));
    }
}
