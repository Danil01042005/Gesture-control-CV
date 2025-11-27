package fs.mtuci.gestures.service;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Сервис низкоуровневого управления системными действиями.
 * Сейчас отвечает за управление громкостью через виртуальные клавиши.
 */
@Service
public class SystemControlService {

    private static final Logger log = LoggerFactory.getLogger(SystemControlService.class);

    private static final byte VK_VOLUME_UP = (byte) 0xAF;
    private static final byte VK_VOLUME_DOWN = (byte) 0xAE;
    private static final byte VK_VOLUME_MUTE = (byte) 0xAD;
    private static final int KEYEVENTF_EXTENDEDKEY = 0x0001;
    private static final int KEYEVENTF_KEYUP = 0x0002;

    private interface User32 extends StdCallLibrary {
        void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);
    }

    private final User32 user32;
    private final boolean available;

    public SystemControlService() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (!windows) {
            log.warn("SystemControlService: поддерживается только Windows, текущая ОС '{}'", System.getProperty("os.name"));
            this.user32 = null;
            this.available = false;
            return;
        }
        User32 api = null;
        boolean ok = false;
        try {
            api = Native.load("user32", User32.class);
            ok = true;
        } catch (UnsatisfiedLinkError ex) {
            log.error("Не удалось загрузить user32.dll для системного управления", ex);
        }
        this.user32 = api;
        this.available = ok;
        if (available) {
            log.info("SystemControlService: WinAPI user32 загружен, управление громкостью доступно");
        }
    }

    public void volumeUp() {
        tap(VK_VOLUME_UP);
    }

    public void volumeDown() {
        tap(VK_VOLUME_DOWN);
    }

    public void volumeMute() {
        tap(VK_VOLUME_MUTE);
    }

    private void tap(byte vk) {
        if (!available || user32 == null) {
            log.debug("SystemControlService: попытка выполнить действие {}, но WinAPI недоступен", vk);
            return;
        }
        user32.keybd_event(vk, (byte) 0, KEYEVENTF_EXTENDEDKEY, 0);
        user32.keybd_event(vk, (byte) 0, KEYEVENTF_EXTENDEDKEY | KEYEVENTF_KEYUP, 0);
    }
}

