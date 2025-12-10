package fs.mtuci.gestures.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.springframework.stereotype.Component;
import fs.mtuci.gestures.service.PythonGestureService;

import java.net.URL;
import java.util.ResourceBundle;


@Component
public class MainWindowController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);

    private final PythonGestureService pythonGestureService;

    private boolean isRecognitionRunning = false;

    private double xOffset = 0;
    private double yOffset = 0;

    private Image playIcon;
    private Image stopIcon;

    public MainWindowController(PythonGestureService pythonGestureService) {
        this.pythonGestureService = pythonGestureService;
    }

    @FXML private Button controlButton;
    @FXML private Label statusLabel;
    @FXML private Label gestureLabel;
    @FXML private Label actionLabel;
    @FXML private HBox titleBar;
    @FXML private Button minimizeButton;
    @FXML private Button closeButton;
    @FXML private BorderPane borderPane;
    @FXML private ImageView controlButtonIcon;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadIcons();

        updateUIForStoppedState();

        setupWindowDragging();

        setupRoundedCorners();

        controlButton.setFocusTraversable(false);
    }

    private void loadIcons() {
        try {
            playIcon = new Image(
                    getClass().getResourceAsStream("/images/play-button.png"));

            stopIcon = new Image(
                    getClass().getResourceAsStream("/images/pause-button.png"));

        } catch (Exception e) {
            logger.error("Ошибка загрузки иконок", e);
        }
    }

    private void setupWindowDragging() {
        titleBar.setOnMousePressed((MouseEvent event) -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged((MouseEvent event) -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    private void setupRoundedCorners() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(borderPane.widthProperty());
        clip.heightProperty().bind(borderPane.heightProperty());
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        borderPane.setClip(clip);
    }

    @FXML
    private void minimizeWindow() {
        Stage stage = (Stage) minimizeButton.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @FXML
    private void onControlButtonClick() {
        if (!isRecognitionRunning) {
            startGestureRecognition();
        } else {
            stopGestureRecognition();
        }
    }

    private void startGestureRecognition() {
        statusLabel.setText("Запуск распознавания...");
        logger.info("Запуск распознавания жестов");

        try {
            boolean started = pythonGestureService.startGestureRecognition(gestureResult -> {
                javafx.application.Platform.runLater(() -> {
                    gestureLabel.setText(gestureResult.toString());
                    statusLabel.setText("Распознавание активно");
                    logger.info("Распознан жест: {}", gestureResult);

                    handleGesture(gestureResult.getGesture());
                });
            });

            if (started) {
                isRecognitionRunning = true;
                updateUIForRunningState();
                statusLabel.setText("Распознавание запущено");
                logger.info("Распознавание жестов успешно запущено");
            } else {
                statusLabel.setText("Ошибка запуска процесса");
                logger.error("Не удалось запустить Python процесс распознавания");
            }

        } catch (Exception e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
            logger.error("Ошибка при запуске распознавания", e);
        }
    }

    private void stopGestureRecognition() {
        statusLabel.setText("Остановка распознавания...");
        logger.info("Остановка распознавания жестов");

        pythonGestureService.stopGestureRecognition();

        isRecognitionRunning = false;
        updateUIForStoppedState();

        statusLabel.setText("Распознавание остановлено");
        logger.info("Распознавание жестов остановлено");
    }

    private void handleGesture(String gesture) {
        String action;
        switch (gesture.toLowerCase()) {
            case "palm":
                action = "Стоп / Отмена";
                break;
            case "fist":
                action = "Клик / Выбор";
                break;
            case "thumb_up":
                action = "Одобрено / Да";
                break;
            case "peace":
                action = "Пауза / Ожидание";
                break;
            case "pointing":
                action = "Наведение курсора";
                break;
            default:
                action = "Жест: " + gesture;
        }
        actionLabel.setText(action);
    }

    private void updateUIForStoppedState() {
        if (playIcon != null) {
            controlButtonIcon.setImage(playIcon);
        }

        controlButton.setText("Запустить распознавание");
        controlButton.getStyleClass().remove("stop-button");
        controlButton.getStyleClass().add("primary-button");
        statusLabel.setText("Ожидание запуска");
        gestureLabel.setText("—");
        actionLabel.setText("—");
    }

    private void updateUIForRunningState() {
        if (stopIcon != null) {
            controlButtonIcon.setImage(stopIcon);
        }

        controlButton.setText("Остановить распознавание");
        controlButton.getStyleClass().remove("primary-button");
        controlButton.getStyleClass().add("stop-button");
        statusLabel.setText("Распознавание активно");
    }

    public void shutdown() {
        if (isRecognitionRunning) {
            stopGestureRecognition();
        }
    }
}