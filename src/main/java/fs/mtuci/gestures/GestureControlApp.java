package fs.mtuci.gestures;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Random;

public class GestureControlApp extends Application {

    private static ConfigurableApplicationContext springContext;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);

        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);

        // --- Слой для свечения ---
        Pane glowLayer = new Pane();
        glowLayer.setPickOnBounds(false);
        glowLayer.setPrefSize(scene.getWidth(), scene.getHeight());

        // --- Первый неровный светящийся круг (синий) ---
        Path blueGlow = createIrregularGlow(
                330, 300, // позиция
                "#1388BF", // цвет
                100, // радиус
                5,   // количество точек
                1000, // размытие
                1000, // радиус свечения
                0.25  // прозрачность
        );
        blueGlow.setScaleX(3);
        blueGlow.setScaleY(3);

        // --- Второй неровный светящийся круг (оранжевый) ---
        Path orangeGlow = createIrregularGlow(
                1300, 550, // позиция (справа)
                "#4B2C79", // цвет
                100,
                5,
                1000,
                1000,
                0.25
        );
        orangeGlow.setScaleX(3);
        orangeGlow.setScaleY(3);

        // Добавляем оба на слой
        glowLayer.getChildren().addAll(blueGlow, orangeGlow);

        // Добавляем слой свечения в root
        if (root instanceof Pane rootPane) {
            rootPane.getChildren().add(glowLayer);
        }
        glowLayer.toBack();

        primaryStage.setTitle("Gesture Control - Управление жестами");
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("");
        primaryStage.show();
    }

    /** Создаёт неровный круг с эффектом свечения */
    private Path createIrregularGlow(double centerX, double centerY, String colorHex,
                                     double radius, int points, double blurRadius,
                                     double glowRadius, double opacity) {
        Random random = new Random();
        Path shape = new Path();
        shape.setFill(Color.web(colorHex));
        shape.setOpacity(opacity);

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double r = radius + random.nextDouble() * 20 - 15;
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
        glow.setSpread(0.25);
        GaussianBlur blur = new GaussianBlur(blurRadius);
        glow.setInput(blur);
        shape.setEffect(glow);

        return shape;
    }

    @Override
    public void stop() throws Exception {
        springContext.close();
    }

    public static void main(String[] args) {
        springContext = SpringApplication.run(GesturesApplication.class, args);
        launch(args);
    }
}
