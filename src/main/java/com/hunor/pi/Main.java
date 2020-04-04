package com.hunor.pi;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollBar;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Main extends Application {
    Stage stage;
    GraphicsContext gc;
    ScrollBar digitBar;
    Button detailButton;

    MediaPlayer clack;
    Image pi;

    AnimationTimer timer;
    NumberFormat formatter;
    EventHandler<KeyEvent> startEvent, digitChange;
    EventHandler<MouseEvent> clickStartEvent;

    double height = 600;
    double width = 1500;
    Block block1;
    Block block2;

    int digits = 8;
    long collisions;
    long timeSteps;
    boolean detailed = false;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        Canvas canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        gc.setTextBaseline(VPos.CENTER);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setStroke(Color.rgb(175, 175, 175));

        AnchorPane root = new AnchorPane();
        root.getChildren().add(canvas);

        ScrollBar digitBar = new ScrollBar();
        this.digitBar = digitBar;
        digitBar.setValue(digits);
        digitBar.setMin(1);
        digitBar.setMax(15);
        digitBar.setBlockIncrement(1);
        digitBar.setPrefWidth(width);
        digitBar.setPrefHeight(32);
        digitBar.setBackground(new Background(new BackgroundFill(Color.rgb(95, 95, 95), null, null)));
        digitBar.valueProperty().addListener((observableValue, oldV, newV) -> {
            if (newV == null || newV.intValue() == oldV.intValue()) {
                return;
            }
            digits = newV.intValue();
            block2.m = Math.pow(100, digits-1);
            formatter = new DecimalFormat("0".repeat(digits));
            collisions = 0;
            draw();
            writeFeedBackFromDigits();
        });
        root.getChildren().add(digitBar);

        detailButton = new Button("Részletes");
        detailButton.setPrefSize(115, 45);
        AnchorPane.setTopAnchor(detailButton, digitBar.getPrefHeight() + 5);
        AnchorPane.setLeftAnchor(detailButton, 5.0);
        detailButton.setFocusTraversable(false);
        detailButton.setFont(new Font("Arial", 18));
        root.getChildren().add(detailButton);
        detailButton.setOnAction(event -> {
            detailed = !detailed;

            if (detailed) detailButton.setEffect(new InnerShadow());
            else detailButton.setEffect(null);
        });

        loadResources();
        setup();
        draw();
        writeFeedBackFromDigits();

        clickStartEvent = mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY &&
                    mouseEvent.getSceneX() > 1100 && mouseEvent.getSceneX() < 1400 && mouseEvent.getSceneY() < height-5 && mouseEvent.getSceneY() > height-305) {
                startSimulation();
            }
        };
        startEvent = keyEvent -> {
            if (keyEvent.getCode() == KeyCode.SPACE || keyEvent.getCode() == KeyCode.ENTER) {
                startSimulation();
            }
        };
        digitChange = keyEvent -> {
            if (keyEvent.getCode() == KeyCode.UP) {
                digitBar.increment();
            }
            else if (keyEvent.getCode() == KeyCode.DOWN) {
                digitBar.decrement();
            }
        };

        timer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                if (!(block1.v > 0 && block2.v > 0 && block1.v < block2.v)) {
                    if (detailed)
                        detailedUpdate();
                    else update();
                    draw();
                }
                else {
                    stopTimer(this);
                }
            }
        };

        //<editor-fold desc="Stage">
        stage.setScene(new Scene(root));
        stage.addEventHandler(KeyEvent.KEY_PRESSED, startEvent);
        stage.addEventHandler(KeyEvent.KEY_PRESSED, digitChange);
        stage.addEventHandler(MouseEvent.MOUSE_CLICKED, clickStartEvent);
        stage.setTitle("Pi");
        stage.getIcons().add(pi);
        stage.setResizable(false);
        stage.show();
        //</editor-fold>
    }

    private void stopTimer(AnimationTimer animationTimer) {
        animationTimer.stop();
        try {
            Thread.sleep(800);
        } catch (InterruptedException ignored) {};

        AnchorPane.setTopAnchor(detailButton, digitBar.getPrefHeight() + 5);
        digitBar.setVisible(true);
        digitBar.setDisable(false);

        block1.x = 700;
        block2.x = 1100;

        draw();
        writeFeedBackFromDigits();

        stage.addEventHandler(KeyEvent.KEY_PRESSED, startEvent);
        stage.addEventHandler(MouseEvent.MOUSE_CLICKED, clickStartEvent);
        stage.addEventHandler(KeyEvent.KEY_PRESSED, digitChange);
    }

    void loadResources() {
        clack = new MediaPlayer(new Media(String.valueOf(getClass().getResource("/clack.wav"))));
        pi = new Image(getClass().getResourceAsStream("/pi.png"));
    }

    void startSimulation() {
        stage.removeEventHandler(MouseEvent.MOUSE_CLICKED, clickStartEvent);
        stage.removeEventHandler(KeyEvent.KEY_PRESSED, startEvent);
        stage.removeEventHandler(KeyEvent.KEY_PRESSED, digitChange);

        digitBar.setDisable(true);
        digitBar.setVisible(false);
        AnchorPane.setTopAnchor(detailButton, 5.0);

        setup();

        timer.start();
    }

    void setup() {
        collisions = 0;
        timeSteps = digits < 3 ? 1 : (int) (Math.pow(8, digits-2));
        formatter = new DecimalFormat("0".repeat(digits));
        block1 = new Block(700, 90, 1, 0, 0);
        block2 = new Block(1100, 300, Math.pow(100, digits-1), -4.0, 90);
    }

    void update() {
        if (block1.x + block1.v >= 0 && block2.x - (block1.x + block1.w) >= block1.v + block2.v) {
            block1.update();
            block2.update();
            if (checkCollides()) {
                clack.stop();
                clack.play();
            }
        }
        else {
            boolean canClack = false;

            block1.v /= timeSteps;
            block2.v /= timeSteps;
            for (int i = 0; i < timeSteps; i++) {
                block1.update();
                block2.update();
                boolean collide = checkCollides();
                canClack = canClack || collide;
            }
            block1.v *= timeSteps;
            block2.v *= timeSteps;
            if (canClack) {
                clack.stop();
                clack.play();
            }
        }
    }

    void detailedUpdate() {
        if (block1.x + block1.v >= 0 && block2.x - (block1.x + block1.w) >= block1.v + block2.v) {
            block1.update();
            block2.update();
            if (checkCollides()) {
                clack.stop();
                clack.play();
            }
        }
        else {
            block1.v /= timeSteps;
            block2.v /= timeSteps;
            for (int i = 0; i < timeSteps; i++) {
                block1.update();
                block2.update();
                if (checkCollides()) {
                    clack.stop();
                    clack.play();
                    block1.v *= timeSteps;
                    block2.v *= timeSteps;
                    return;
                }
            }
            block1.v *= timeSteps;
            block2.v *= timeSteps;
        }
    }

    boolean checkCollides() {
        if (block1.hitWall()) {
            block1.reverse();
            collisions += 1;
            return true;
        }
        if (block1.collide(block2)) {
            double v1 = block1.bounce(block2);
            double v2 = block2.bounce(block1);
            block1.v = v1;
            block2.v = v2;
            collisions += 1;
            return true;
        }
        else return false;
    }

    void draw() {
        gc.setFill(Color.rgb(75, 75, 75));
        gc.fillRect(0, 0, width, height);
        gc.setFill(Color.rgb(225, 225, 255));
        gc.setFont(new Font("Arial Bold", 72));
        gc.fillText(formatter.format(collisions), width/2, height/3);

        block1.show(Color.rgb(185, 50, 50));
        block2.show(Color.rgb(50, 50, 185));
    }

    void writeFeedBackFromDigits() {
        gc.setFill(Color.rgb(225, 225, 255));
        gc.setLineWidth(1);
        gc.setFont(new Font("Arial Bold", 32));
        gc.fillText("Pi " + digits + " számjegyének keresése.", width/2, height/9);
    }

    class Block {
        double x, y, w, v, m, xConst;

        Block(double x, double w, double m, double v, double xConst) {
            this.x = x;
            this.y = height - w;
            this.w = w;
            this.m = m;
            this.v = v;
            this.xConst = xConst;
        }

        void reverse() {
            this.v *= -1;
        }

        boolean hitWall() {
            return this.x <= 0;
        }

        boolean collide(Block other) {
            return !(this.x + this.w < other.x ||
                    this.x > other.x + other.w);
        }
        
        double bounce(Block other) {
            double sumM = this.m + other.m;
            return (this.m-other.m)/sumM * this.v + (2*other.m/sumM) * other.v;
        }

        void update() {
            x += v;
        }

        void show(Color color) {
            gc.setFill(color);
            gc.setLineWidth(3);
            gc.fillRect(Math.max(x, xConst), y, w, w);
            gc.strokeRect(Math.max(x, xConst), y, w, w);
            gc.setFill(Color.rgb(225, 225, 255));
            gc.setFont(new Font("Arial", 22));
            gc.fillText((m == 1 ? "1" : m == 100 ? "100" : "100^"+(digits-1)) + " kg", Math.max(x, xConst) + w/2, y + w/2);
        }
    }

}
