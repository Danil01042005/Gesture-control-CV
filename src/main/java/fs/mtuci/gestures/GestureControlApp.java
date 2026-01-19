package fs.mtuci.gestures;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import fs.mtuci.gestures.service.CameraService;

public class GestureControlApp extends Application {

    private static ConfigurableApplicationContext springContext;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        primaryStage.setTitle("Gesture Control - Управление жестами");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        // Освобождаем камеру перед закрытием приложения
        if (springContext != null) {
            try {
                CameraService cameraService = springContext.getBean(CameraService.class);
                if (cameraService != null) {
                    cameraService.release();
                }
            } catch (Exception e) {
                // Игнорируем ошибки при освобождении
            }
        }
        springContext.close();
    }

    public static void main(String[] args) {
        springContext = SpringApplication.run(GesturesApplication.class, args);
        launch(args);
    }
}