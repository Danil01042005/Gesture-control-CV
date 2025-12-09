package fs.mtuci.gestures.service;

import org.opencv.videoio.Videoio;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelFormat;

import java.nio.ByteBuffer;

@Service
public class CameraService {

    private static final Logger logger = LoggerFactory.getLogger(CameraService.class);

    private VideoCapture capture;
    private Mat bgr;
    private Mat rgb;
    private int width = 640;
    private int height = 480;
    private int cameraIndex = 0;
    private boolean opencvLoaded = false;

    public boolean initializeCamera() {
        try {
            if (!opencvLoaded) {
                System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                opencvLoaded = true;
                logger.info("OpenCV загружен успешно");
            }

            if (bgr == null) bgr = new Mat();
            if (rgb == null) rgb = new Mat();
            
            capture = new VideoCapture();
            if(!capture.open(cameraIndex)) {
                logger.error("Не удалось открыть камеру №" , cameraIndex);
                return false;
            }
            capture.set(Videoio.CAP_PROP_FRAME_WIDTH, width);
            capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, height);
            logger.info("Камера открыта: {}x{}", width, height);
            return true;
        } catch (Throwable t){
            logger.error("Ошибка инициализации OpenCV/камеры", t);
            return false;
        }
    }

    public Mat captureFrame() {
        if (capture == null || !capture.isOpened()) {
            logger.error("Камера не инициализирована");
            return null;
        }
        if (!capture.read(bgr) || bgr.empty()) {
            logger.warn("Кадр не получен");
            return null;
        }
        return bgr;
    }

    public Image toFxImage(Mat matBgr) {
        if (matBgr == null || matBgr.empty()) return null;

        Imgproc.cvtColor(matBgr, rgb, Imgproc.COLOR_BGR2RGB);
        int cols = rgb.cols();
        int rows = rgb.rows();
        int channels = rgb.channels();
        int bufferSize = cols * rows * channels;
        byte[] buffer = new byte[bufferSize];
        rgb.get(0, 0, buffer);
        WritableImage image = new WritableImage(cols, rows);
        PixelWriter pw = image.getPixelWriter();
        pw.setPixels(0, 0, cols, rows,
                PixelFormat.getByteRgbInstance(),
                buffer, 0, cols * channels
        );
        return image;
    }

    public boolean isOpened() {
        return capture != null && capture.isOpened();
    }

    public void release() {
        try {
            if (capture != null) {
                capture.release();
                capture = null;
            }
            if (bgr != null) {
                bgr.release();
                bgr = null;
            }
            if (rgb != null) {
                rgb.release();
                rgb = null;
            }
            logger.info("Камера и ресурсы освобождены");
        } catch (Exception e) {
            logger.warn("Ошибка при освобождении ресурсов", e);
        }
    }

    public void setCameraIndex(int cameraIndex) { this.cameraIndex = cameraIndex; }
    public void setResolution(int width, int height) { this.width = width; this.height = height; }
}
