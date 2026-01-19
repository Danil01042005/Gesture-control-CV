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
import fs.mtuci.gestures.service.PythonGestureService;
import fs.mtuci.gestures.service.SystemControlService;

import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;


@Component
public class MainWindowController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);

    
    private final CameraService cameraService;
    
    private final PythonGestureService pythonGestureService;
    private final SystemControlService systemControlService;

    private static final long ACTION_COOLDOWN_MS = 750;
    private final Map<String, Long> actionTimestamps = new HashMap<>();

    public MainWindowController(CameraService cameraService,
                                PythonGestureService pythonGestureService,
                                SystemControlService systemControlService) {
        this.cameraService = cameraService;
        this.pythonGestureService = pythonGestureService;
        this.systemControlService = systemControlService;
    }

    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button settingsButton;

    @FXML private ImageView cameraPreview;
    @FXML private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusLabel.setText("Готов к работе!");
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    @FXML
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
        }
    }

    @FXML
    private void onStopButtonClick() {
        statusLabel.setText("Остановка распознавания жестов...");
        logger.info("Остановка управления жестами");
        
        pythonGestureService.stopGestureRecognition();
        cameraService.release(); // Освобождаем камеру
        
        startButton.setDisable(false);
        stopButton.setDisable(true);
        gestureLabel.setText("—");
        statusLabel.setText("Распознавание остановлено");
    }

    /**
     * Обрабатывает различные жесты
     */
    private void handleGesture(String gesture) {
        if (gesture == null) {
            return;
        }
        String gestureLower = gesture.toLowerCase().trim();
        logger.info("Обработка жеста: '{}' (нормализован: '{}')", gesture, gestureLower);

        switch (gestureLower) {
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
            case "big_finger_top":
                actionLabel.setText("Действие: Громкость +");
                triggerAction("volume_up", systemControlService::volumeUp);
                break;
            case "big_finger_bottom":
            case "big_finger_down":
                actionLabel.setText("Действие: Громкость −");
                triggerAction("volume_down", systemControlService::volumeDown);
                break;
            case "palm_mute":
                actionLabel.setText("Действие: Mute");
                triggerAction("volume_mute", systemControlService::volumeMute);
            // Swipe жесты для перемотки
            case "swipeleft":
                actionLabel.setText("Действие: Перемотка назад");
                logger.info("Выполняется перемотка назад");
                triggerAction("seek_backward", systemControlService::seekBackward);
                break;
            case "swiperight":
                actionLabel.setText("Действие: Перемотка вперед");
                logger.info("Выполняется перемотка вперед");
                triggerAction("seek_forward", systemControlService::seekForward);
                break;
            // Swipe жесты для скролла
            case "swipeup":
                actionLabel.setText("Действие: Скролл вверх");
                logger.info("Выполняется скролл вверх");
                triggerAction("scroll_up", systemControlService::scrollUp);
                break;
            case "swipedown":
                actionLabel.setText("Действие: Скролл вниз");
                logger.info("Выполняется скролл вниз");
                triggerAction("scroll_down", systemControlService::scrollDown);
                break;
            // Rotate жесты для управления громкостью
            case "rotateleft":
                actionLabel.setText("Действие: Уменьшить громкость");
                logger.info("Выполняется уменьшение громкости");
                triggerAction("volume_down", systemControlService::volumeDown);
                break;
            case "rotateright":
                actionLabel.setText("Действие: Увеличить громкость");
                logger.info("Выполняется увеличение громкости");
                triggerAction("volume_up", systemControlService::volumeUp);
                break;
            default:
                logger.warn("Неизвестный жест: '{}' (нормализован: '{}')", gesture, gestureLower);
                actionLabel.setText("Действие: " + gesture);
        }
    }



    @FXML
    private void onSettingsButtonClick() {
        statusLabel.setText("Настройки в разработке");
    }

    @FXML private Label gestureLabel;
    @FXML private Label actionLabel;
    @FXML private ToggleButton pauseButton;

    private void triggerAction(String key, Runnable action) {
        long now = System.currentTimeMillis();
        Long last = actionTimestamps.get(key);
        if (last == null || now - last >= ACTION_COOLDOWN_MS) {
            actionTimestamps.put(key, now);
            try {
                action.run();
                logger.info("Выполнено действие {}", key);
            } catch (Exception ex) {
                logger.error("Ошибка при выполнении действия {}", key, ex);
            }
        } else {
            logger.debug("Пропущено действие {} (cooldown)", key);
        }
    }

}
