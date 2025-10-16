package fs.mtuci.gestures.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;


@Component
public class MainWindowController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);

    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button settingsButton;

    @FXML private ImageView cameraPreview;
    @FXML private Label statusLabel;
    @FXML private Label fpsLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusLabel.setText("Готов к работе!");
        fpsLabel.setText("FPS: --");
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    @FXML
    private void onStartButtonClick() {
        statusLabel.setText("Управление жестами запущено - в разработке");
        logger.info("Запуск обработки жестов");
        try {
            startButton.setDisable(true);
            stopButton.setDisable(false);
        } catch (Exception e) {
            statusLabel.setText("Ошибка запуска");
            logger.error("Ошибка при запуске обработки", e);
        }
    }

    @FXML
    private void onStopButtonClick() {
        statusLabel.setText("Управление жестами отключено - В разработке");
        logger.info("Остановка управления жестами");
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    @FXML
    private void onSettingsButtonClick() {
        statusLabel.setText("Настройки в разработке");
    }
}
