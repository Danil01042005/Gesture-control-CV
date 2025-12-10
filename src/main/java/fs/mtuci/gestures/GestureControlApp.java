package fs.mtuci.gestures;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import fs.mtuci.gestures.controller.MainWindowController;

import java.util.Random;

public class GestureControlApp extends Application {

    private static ConfigurableApplicationContext springContext;
    private MainWindowController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);

        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 500, 600);

        controller = fxmlLoader.getController();

        scene.setFill(Color.TRANSPARENT);

        addGlowingShapes(root);

        primaryStage.initStyle(StageStyle.TRANSPARENT);

        primaryStage.setTitle("Gesture Control - Управление жестами");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(550);

        primaryStage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.shutdown();
            }
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    private void addGlowingShapes(Parent root) {
        if (root instanceof Pane pane) {
            Path purpleGlow = createIrregularGlow(
                    100, 100,
                    "#4B2C79",
                    80,
                    8,
                    300,
                    200,
                    0.25
            );

            purpleGlow.setScaleX(1.5);
            purpleGlow.setScaleY(1.5);

            Path blueGlow = createIrregularGlow(
                    430, 380,
                    "#1388BF",
                    70,
                    8,
                    300,
                    180,
                    0.20
            );

            blueGlow.setScaleX(1.8);
            blueGlow.setScaleY(1.8);

            pane.getChildren().add(0, blueGlow);
            pane.getChildren().add(1, purpleGlow);
        }
    }

    private Path createIrregularGlow(double centerX, double centerY, String colorHex,
                                     double radius, int points, double blurRadius,
                                     double glowRadius, double opacity) {
        Random random = new Random();
        Path shape = new Path();
        shape.setFill(Color.web(colorHex));
        shape.setOpacity(opacity);

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double r = radius + random.nextDouble() * 40 - 20;
            double x = centerX + r * Math.cos(angle);
            double y = centerY + r * Math.sin(angle);

            if (i == 0) {
                shape.getElements().add(new MoveTo(x, y));
            } else {
                shape.getElements().add(new LineTo(x, y));
            }
        }
        shape.getElements().add(new ClosePath());

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(colorHex));
        glow.setRadius(glowRadius);
        glow.setSpread(0.3);

        GaussianBlur blur = new GaussianBlur(blurRadius);
        glow.setInput(blur);
        shape.setEffect(glow);

        return shape;
    }

    @Override
    public void stop() throws Exception {
        if (controller != null) {
            controller.shutdown();
        }
        springContext.close();
    }

    public static void main(String[] args) {
        springContext = SpringApplication.run(GesturesApplication.class, args);
        launch(args);
    }
}