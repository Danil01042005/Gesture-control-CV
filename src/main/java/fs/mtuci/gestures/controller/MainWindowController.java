package fs.mtuci.gestures.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
<<<<<<< HEAD
=======

>>>>>>> origin/feature/andrey
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import org.springframework.stereotype.Component;
import fs.mtuci.gestures.service.CameraService;
<<<<<<< HEAD

=======
import fs.mtuci.gestures.service.PythonGestureService;
>>>>>>> origin/feature/andrey

import java.net.URL;
import java.util.ResourceBundle;

<<<<<<< HEAD
=======

>>>>>>> origin/feature/andrey
@Component
public class MainWindowController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);
<<<<<<< HEAD
    private final CameraService cameraService;

    public MainWindowController(CameraService cameraService) {
        this.cameraService = cameraService;
=======

    
    private final CameraService cameraService;
    
    private final PythonGestureService pythonGestureService;

    public MainWindowController(CameraService cameraService, PythonGestureService pythonGestureService) {
        this.cameraService = cameraService;
        this.pythonGestureService = pythonGestureService;
>>>>>>> origin/feature/andrey
    }

    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button settingsButton;
<<<<<<< HEAD
    @FXML private ImageView cameraPreview;
    @FXML private Label statusLabel;
    @FXML private Label gestureLabel;
    @FXML private Label actionLabel;
    @FXML private ToggleButton pauseButton;
=======

    @FXML private ImageView cameraPreview;
    @FXML private Label statusLabel;
>>>>>>> origin/feature/andrey

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusLabel.setText("Готов к работе!");
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    @FXML
<<<<<<< HEAD
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
=======
    private void onStartButtonClick() {
        statusLabel.setText("Запуск распознавания жестов...");
        logger.info("Запуск обработки жестов");
        
        try {
            boolean started = pythonGestureService.startGestureRecognition(gestureResult -> {
                // Этот код выполняется при каждом распознанном жесте
                javafx.application.Platform.runLater(() -> {
                    gestureLabel.setText(gestureResult.toString());
                    statusLabel.setText("Распознавание активно");
                    logger.info("Распознан жест: {}", gestureResult);
                    
                    // Здесь можно добавить действия для разных жестов
                    handleGesture(gestureResult.getGesture());
                });
            });
            
            if (started) {
                startButton.setDisable(true);
                stopButton.setDisable(false);
                statusLabel.setText("Распознавание жестов запущено!");
            } else {
                statusLabel.setText("Ошибка запуска Python процесса");
            }
            
        } catch (Exception e) {
            statusLabel.setText("Ошибка запуска: " + e.getMessage());
            logger.error("Ошибка при запуске обработки", e);
>>>>>>> origin/feature/andrey
        }
    }

    @FXML
    private void onStopButtonClick() {
<<<<<<< HEAD
        logger.info("Остановка камеры");
        cameraService.stopCamera();
        statusLabel.setText("Камера остановлена");
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

=======
        statusLabel.setText("Остановка распознавания жестов...");
        logger.info("Остановка управления жестами");
        
        pythonGestureService.stopGestureRecognition();
        
        startButton.setDisable(false);
        stopButton.setDisable(true);
        gestureLabel.setText("—");
        statusLabel.setText("Распознавание остановлено");
    }

    /**
     * Обрабатывает различные жесты
     */
    private void handleGesture(String gesture) {
        switch (gesture.toLowerCase()) {
            case "palm":
                actionLabel.setText("Действие: Стоп");
                break;
            case "fist":
                actionLabel.setText("Действие: Клик");
                break;
            case "thumb_up":
                actionLabel.setText("Действие: Одобрено");
                break;
            case "peace":
                actionLabel.setText("Действие: Пауза");
                break;
            default:
                actionLabel.setText("Действие: " + gesture);
        }
    }



>>>>>>> origin/feature/andrey
    @FXML
    private void onSettingsButtonClick() {
        statusLabel.setText("Настройки в разработке");
    }

<<<<<<< HEAD
=======
    @FXML private Label gestureLabel;
    @FXML private Label actionLabel;
    @FXML private ToggleButton pauseButton;

>>>>>>> origin/feature/andrey
}
