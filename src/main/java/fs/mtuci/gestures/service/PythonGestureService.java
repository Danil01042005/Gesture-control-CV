package fs.mtuci.gestures.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

@Service
public class PythonGestureService {

    private static final Logger logger = LoggerFactory.getLogger(PythonGestureService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private Process pythonProcess;
    private Thread readerThread;
    private volatile boolean running = false;

    /**
     * Запускает Python процесс распознавания жестов
     * @param onGestureDetected Callback для обработки распознанных жестов
     * @return true если успешно запущен
     */
    public boolean startGestureRecognition(Consumer<GestureResult> onGestureDetected) {
        if (running) {
            logger.warn("Python процесс уже запущен");
            return false;
        }

        try {
            // Путь к Python интерпретатору из venv
            String pythonPath = findPythonExecutable();
            String scriptPath = "python/processor.py";
            
            logger.info("Запуск Python скрипта: {} с интерпретатором: {}", scriptPath, pythonPath);
            
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            
            pythonProcess = pb.start();
            running = true;
            
            // Читаем вывод Python в отдельном потоке
            readerThread = new Thread(() -> readPythonOutput(onGestureDetected));
            readerThread.setDaemon(true);
            readerThread.start();
            
            logger.info("Python процесс успешно запущен");
            return true;
            
        } catch (IOException e) {
            logger.error("Ошибка запуска Python процесса", e);
            return false;
        }
    }

    /**
     * Останавливает Python процесс
     */
    public void stopGestureRecognition() {
        running = false;
        
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
            try {
                pythonProcess.waitFor();
            } catch (InterruptedException e) {
                pythonProcess.destroyForcibly();
            }
            logger.info("Python процесс остановлен");
        }
        
        pythonProcess = null;
    }

    /**
     * Читает вывод из Python процесса
     */
    private void readPythonOutput(Consumer<GestureResult> callback) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(pythonProcess.getInputStream()))) {
            
            String line;
            while (running && (line = reader.readLine()) != null) {
                logger.debug("Python output: {}", line);
                
                // Пытаемся распарсить JSON
                if (line.trim().startsWith("{")) {
                    try {
                        JsonNode node = objectMapper.readTree(line);
                        
                        // Проверяем наличие всех необходимых полей
                        if (!node.has("gesture") || !node.has("confidence") || !node.has("timestamp")) {
                            logger.warn("Неполный JSON от Python: {}", line);
                            continue;
                        }
                        
                        String gesture = node.get("gesture").asText();
                        
                        // Проверяем, что жест не пустой
                        if (gesture == null || gesture.trim().isEmpty()) {
                            logger.debug("Пустой жест, пропускаем");
                            continue;
                        }
                        
                        double confidence = node.get("confidence").asDouble();
                        
                        // timestamp может быть как double (time.time()) или long
                        long timestamp;
                        if (node.get("timestamp").isDouble()) {
                            timestamp = (long) (node.get("timestamp").asDouble() * 1000); // конвертируем в миллисекунды
                        } else {
                            timestamp = node.get("timestamp").asLong();
                        }
                        
                        GestureResult result = new GestureResult(gesture, confidence, timestamp);
                        
                        // Вызываем callback только для уверенных распознаваний
                        if (confidence > 99.0) {
                            logger.info("Принят жест: {} (уверенность: {}%)", gesture, confidence);
                            callback.accept(result);
                        } else {
                            logger.debug("Низкая уверенность ({}%), пропускаем жест: {}", confidence, gesture);
                        }
                        
                    } catch (Exception e) {
                        logger.warn("Ошибка парсинга JSON: {}", line, e);
                    }
                } else {
                    // Обычный текстовый вывод (логи Python)
                    logger.info("Python: {}", line);
                }
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Ошибка чтения вывода Python", e);
            }
        }
    }

    /**
     * Находит Python интерпретатор
     */
    private String findPythonExecutable() {
        // Используем системный Python
        return "python";
    }

    public boolean isRunning() {
        return running && pythonProcess != null && pythonProcess.isAlive();
    }

    /**
     * Класс для хранения результата распознавания жеста
     */
    public static class GestureResult {
        private final String gesture;
        private final double confidence;
        private final long timestamp;

        public GestureResult(String gesture, double confidence, long timestamp) {
            this.gesture = gesture;
            this.confidence = confidence;
            this.timestamp = timestamp;
        }

        public String getGesture() { return gesture; }
        public double getConfidence() { return confidence; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("Жест: %s (%.1f%%)", gesture, confidence);
        }
    }
}