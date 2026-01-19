package fs.mtuci.gestures.service;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Сервис для управления системой через эмуляцию клавиатуры и мыши
 */
@Service
public class SystemControlService {

    private static final Logger logger = LoggerFactory.getLogger(SystemControlService.class);
    private Robot robot;
    private static final long ACTION_COOLDOWN_MS = 300; // Задержка между действиями
    private long lastScrollTime = 0;
    private long lastSeekTime = 0;
    private long lastVolumeTime = 0;
    
    // Коды клавиш Windows
    private static final byte VK_LEFT = (byte) 0x25;
    private static final byte VK_RIGHT = (byte) 0x27;
    private static final byte VK_UP = (byte) 0x26;
    private static final byte VK_DOWN = (byte) 0x28;
    private static final byte VK_MENU = (byte) 0x12; // Alt key
    // Мультимедийные коды не используем для браузера — остаемся на стрелках
    private static final byte VK_PRIOR = (byte) 0x21; // Page Up
    private static final byte VK_NEXT = (byte) 0x22; // Page Down
    private static final byte VK_VOLUME_UP = (byte) 0xAF;
    private static final byte VK_VOLUME_DOWN = (byte) 0xAE;
    private static final byte VK_VOLUME_MUTE = (byte) 0xAD;
    
    private static final int KEYEVENTF_KEYUP = 0x0002;
    
    // Для скролла через mouse_event
    private static final int MOUSEEVENTF_WHEEL = 0x0800;
    private static final int WHEEL_DELTA = 120;
    
    // Интерфейс для User32 API
    private interface WinUser32 extends StdCallLibrary {
        WinUser32 INSTANCE = Native.load("user32", WinUser32.class);
        void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);
        void mouse_event(int dwFlags, int dx, int dy, int dwData, int dwExtraInfo);
    }

    public SystemControlService() {
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                robot = new Robot();
                robot.setAutoDelay(10);
                robot.setAutoWaitForIdle(false);
                logger.info("Robot инициализирован для управления системой");
            } else {
                logger.warn("Графическая среда недоступна (headless режим), Robot не может быть создан");
            }
        } catch (AWTException e) {
            logger.error("Не удалось инициализировать Robot для управления системой", e);
        }
    }

    /**
     * Скролл вверх через клавишу UP (работает на тачпаде)
     * Увеличена чувствительность: 3 нажатия вместо 1
     */
    public void scrollUp() {
        long now = System.currentTimeMillis();
        if (now - lastScrollTime < ACTION_COOLDOWN_MS) {
            logger.debug("Скролл вверх пропущен (cooldown)");
            return;
        }
        lastScrollTime = now;
        
        try {
            logger.info("СКРОЛЛ ВВЕРХ: отправка клавиши UP (3 раза)");
            
            // Используем JNA для отправки клавиши UP - работает на тачпаде
            // Увеличена чувствительность: отправляем 3 нажатия
            for (int i = 0; i < 3; i++) {
                WinUser32.INSTANCE.keybd_event(VK_UP, (byte) 0, 0, 0);
                Thread.sleep(30);
                WinUser32.INSTANCE.keybd_event(VK_UP, (byte) 0, KEYEVENTF_KEYUP, 0);
                Thread.sleep(20);
            }
            
            logger.info("СКРОЛЛ ВВЕРХ ВЫПОЛНЕН (клавиша UP отправлена 3 раза через JNA)");
        } catch (UnsatisfiedLinkError e) {
            logger.error("JNA недоступен, пробуем Robot", e);
            // Fallback на Robot
            if (robot != null) {
                for (int i = 0; i < 3; i++) {
                    robot.keyPress(KeyEvent.VK_UP);
                    robot.delay(50);
                    robot.keyRelease(KeyEvent.VK_UP);
                    robot.delay(30);
                }
            }
        } catch (Exception e) {
            logger.error("ОШИБКА при скролле вверх", e);
        }
    }

    /**
     * Скролл вниз через клавишу DOWN (работает на тачпаде)
     * Увеличена чувствительность: 3 нажатия вместо 1
     */
    public void scrollDown() {
        long now = System.currentTimeMillis();
        if (now - lastScrollTime < ACTION_COOLDOWN_MS) {
            logger.debug("Скролл вниз пропущен (cooldown)");
            return;
        }
        lastScrollTime = now;
        
        try {
            logger.info("СКРОЛЛ ВНИЗ: отправка клавиши DOWN (3 раза)");
            
            // Используем JNA для отправки клавиши DOWN - работает на тачпаде
            // Увеличена чувствительность: отправляем 3 нажатия
            for (int i = 0; i < 3; i++) {
                WinUser32.INSTANCE.keybd_event(VK_DOWN, (byte) 0, 0, 0);
                Thread.sleep(30);
                WinUser32.INSTANCE.keybd_event(VK_DOWN, (byte) 0, KEYEVENTF_KEYUP, 0);
                Thread.sleep(20);
            }
            
            logger.info("СКРОЛЛ ВНИЗ ВЫПОЛНЕН (клавиша DOWN отправлена 3 раза через JNA)");
        } catch (UnsatisfiedLinkError e) {
            logger.error("JNA недоступен, пробуем Robot", e);
            // Fallback на Robot
            if (robot != null) {
                for (int i = 0; i < 3; i++) {
                    robot.keyPress(KeyEvent.VK_DOWN);
                    robot.delay(50);
                    robot.keyRelease(KeyEvent.VK_DOWN);
                    robot.delay(30);
                }
            }
        } catch (Exception e) {
            logger.error("ОШИБКА при скролле вниз", e);
        }
    }

    /**
     * Перемотка назад: отправляем стрелку Left (HTML5-плееры в браузере)
     */
    public void seekBackward() {
        long now = System.currentTimeMillis();
        if (now - lastSeekTime < ACTION_COOLDOWN_MS) {
            return;
        }
        lastSeekTime = now;
        
        try {
            logger.info("ПЕРЕМОТКА НАЗАД: отправка LEFT");
            
            // Обычная стрелка влево — стандартный seek в HTML5-плеерах
            WinUser32.INSTANCE.keybd_event(VK_LEFT, (byte) 0, 0, 0);
            Thread.sleep(50);
            WinUser32.INSTANCE.keybd_event(VK_LEFT, (byte) 0, KEYEVENTF_KEYUP, 0);
            
            logger.info("ПЕРЕМОТКА НАЗАД ВЫПОЛНЕНА (LEFT через JNA)");
        } catch (UnsatisfiedLinkError e) {
            logger.error("JNA недоступен, пробуем Robot", e);
            // Fallback на Robot (используем стрелку влево)
            if (robot != null) {
                robot.keyPress(KeyEvent.VK_LEFT);
                robot.delay(60);
                robot.keyRelease(KeyEvent.VK_LEFT);
            }
        } catch (Exception e) {
            logger.error("ОШИБКА при перемотке назад", e);
        }
    }

    /**
     * Перемотка вперед: отправляем стрелку Right (HTML5-плееры в браузере)
     */
    public void seekForward() {
        long now = System.currentTimeMillis();
        if (now - lastSeekTime < ACTION_COOLDOWN_MS) {
            return;
        }
        lastSeekTime = now;
        
        try {
            logger.info("ПЕРЕМOТКА ВПЕРЕД: отправка RIGHT");
            
            // Обычная стрелка вправо — стандартный seek в HTML5-плеерах
            WinUser32.INSTANCE.keybd_event(VK_RIGHT, (byte) 0, 0, 0);
            Thread.sleep(50);
            WinUser32.INSTANCE.keybd_event(VK_RIGHT, (byte) 0, KEYEVENTF_KEYUP, 0);
            
            logger.info("ПЕРЕМОТКА ВПЕРЕД ВЫПОЛНЕНА (RIGHT через JNA)");
        } catch (UnsatisfiedLinkError e) {
            logger.error("JNA недоступен, пробуем Robot", e);
            // Fallback на Robot (используем стрелку вправо)
            if (robot != null) {
                robot.keyPress(KeyEvent.VK_RIGHT);
                robot.delay(60);
                robot.keyRelease(KeyEvent.VK_RIGHT);
            }
        } catch (Exception e) {
            logger.error("ОШИБКА при перемотке вперед", e);
        }
    }

    /**
     * Увеличить громкость
     */
    public void volumeUp() {
        long now = System.currentTimeMillis();
        if (now - lastVolumeTime < ACTION_COOLDOWN_MS) {
            logger.debug("Громкость вверх пропущена (cooldown)");
            return;
        }
        lastVolumeTime = now;
        
        try {
            logger.info("ГРОМКОСТЬ ВВЕРХ: отправка клавиши VOLUME_UP");
            
            // Используем JNA для прямой отправки системной клавиши громкости
            // Для мультимедийных клавиш не нужен KEYEVENTF_EXTENDEDKEY
            WinUser32.INSTANCE.keybd_event(VK_VOLUME_UP, (byte) 0, 0, 0);
            Thread.sleep(50);
            WinUser32.INSTANCE.keybd_event(VK_VOLUME_UP, (byte) 0, KEYEVENTF_KEYUP, 0);
            
            logger.info("ГРОМКОСТЬ ВВЕРХ ВЫПОЛНЕНА");
        } catch (UnsatisfiedLinkError e) {
            logger.error("JNA недоступен для громкости", e);
        } catch (Exception e) {
            logger.error("ОШИБКА при увеличении громкости", e);
        }
    }

    /**
     * Уменьшить громкость
     */
    public void volumeDown() {
        long now = System.currentTimeMillis();
        if (now - lastVolumeTime < ACTION_COOLDOWN_MS) {
            logger.debug("Громкость вниз пропущена (cooldown)");
            return;
        }
        lastVolumeTime = now;
        
        try {
            logger.info("ГРОМКОСТЬ ВНИЗ: отправка клавиши VOLUME_DOWN");
            
            // Используем JNA для прямой отправки системной клавиши громкости
            // Для мультимедийных клавиш не нужен KEYEVENTF_EXTENDEDKEY
            WinUser32.INSTANCE.keybd_event(VK_VOLUME_DOWN, (byte) 0, 0, 0);
            Thread.sleep(50);
            WinUser32.INSTANCE.keybd_event(VK_VOLUME_DOWN, (byte) 0, KEYEVENTF_KEYUP, 0);
            
            logger.info("ГРОМКОСТЬ ВНИЗ ВЫПОЛНЕНА");
        } catch (UnsatisfiedLinkError e) {
            logger.error("JNA недоступен для громкости", e);
        } catch (Exception e) {
            logger.error("ОШИБКА при уменьшении громкости", e);
        }
    }
}
