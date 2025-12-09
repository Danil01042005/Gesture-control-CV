package fs.mtuci.gestures.service;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;

import org.opencv.core.CvType;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

@Service
public class CameraService {

    //инициализация OpenCV
    static {
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    private VideoCapture camera;
    private Thread cameraThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public boolean initializeCamera() {
        if (camera == null) {
            camera = new VideoCapture(0);
        }
        return camera.isOpened();
    }

    public void startCameraStream(ImageView imageView) {
        if (running.get()) return;

        running.set(true);
        cameraThread = new Thread(() -> {
            Mat frame = new Mat();
            while (running.get() && camera.isOpened()) {
                if (camera.read(frame)) {
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);
                    Image image = mat2Image(frame);
                    Platform.runLater(() -> imageView.setImage(image));
                }
            }
            frame.release();
        });
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    public void stopCamera() {
        running.set(false);
        if (camera != null && camera.isOpened()) {
            camera.release();
        }
    }

    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new java.io.ByteArrayInputStream(buffer.toArray()));
    }

    @PreDestroy
    public void cleanup() {
        stopCamera();
    }
}
