package fs.mtuci.gestures;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class OpenCVTest {
    
    public static void main(String[] args) {
        System.out.println("=== Тест OpenCV ===");
        
        try {
            // 1. Загрузка нативной библиотеки
            System.out.println("1. Загрузка OpenCV...");
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("✅ OpenCV загружен успешно");
            
            // 2. Проверка версии
            System.out.println("2. Версия OpenCV: " + Core.VERSION);
            
            // 3. Тест создания Mat
            System.out.println("3. Тест создания Mat...");
            Mat testMat = new Mat();
            System.out.println("✅ Mat создан успешно");
            testMat.release();
            
            // 4. Тест камеры
            System.out.println("4. Тест камеры...");
            VideoCapture capture = new VideoCapture();
            if (capture.open(0)) {
                System.out.println("✅ Камера 0 открыта");
                
                // Проверка разрешения
                double width = capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
                double height = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
                System.out.println("   Разрешение: " + (int)width + "x" + (int)height);
                
                // Тест захвата кадра
                Mat frame = new Mat();
                if (capture.read(frame) && !frame.empty()) {
                    System.out.println("✅ Кадр захвачен: " + frame.cols() + "x" + frame.rows());
                } else {
                    System.out.println("⚠️ Кадр не получен");
                }
                frame.release();
                capture.release();
            } else {
                System.out.println("❌ Камера 0 недоступна");
                
                // Попробуем другие камеры
                for (int i = 1; i <= 3; i++) {
                    if (capture.open(i)) {
                        System.out.println("✅ Камера " + i + " открыта");
                        capture.release();
                        break;
                    }
                }
            }
            
            System.out.println("=== Тест завершен ===");
            
        } catch (UnsatisfiedLinkError e) {
            System.err.println("❌ Ошибка загрузки OpenCV: " + e.getMessage());
            System.err.println("   Нативные библиотеки не найдены");
        } catch (Exception e) {
            System.err.println("❌ Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

