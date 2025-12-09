package fs.mtuci.gestures.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import org.springframework.stereotype.Component;
import fs.mtuci.gestures.service.CameraService;


import java.net.URL;
import java.util.ResourceBundle;

@Component
public class MainWindowController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);
    private final CameraService cameraService;

    public MainWindowController(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button settingsButton;
    @FXML private ImageView cameraPreview;
    @FXML private Label statusLabel;
    @FXML private Label gestureLabel;
    @FXML private Label actionLabel;
    @FXML private ToggleButton pauseButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusLabel.setText("Готов к работе!");
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    @FXML
    private void onLiveCameraButtonClick() {
        statusLabel.setText("Запуск камеры...");
        logger.info("Запуск камеры");

        try {
            if (cameraService.initializeCamera()) {
                cameraService.startCameraStream(cameraPreview);
                statusLabel.setText("Камера активна");
                startButton.setDisable(true);
                stopButton.setDisable(false);
            } else {
                statusLabel.setText("Ошибка: камера не найдена");
            }
        } catch (Exception e) {
            statusLabel.setText("Ошибка запуска камеры");
            logger.error("Ошибка при запуске камеры", e);
        }
    }

    @FXML
    private void onStopButtonClick() {
        logger.info("Остановка камеры");
        cameraService.stopCamera();
        statusLabel.setText("Камера остановлена");
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    @FXML
    private void onSettingsButtonClick() {
        statusLabel.setText("Настройки в разработке");
    }

}
