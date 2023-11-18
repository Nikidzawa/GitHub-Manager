package ru.nikidzawa.github_manager;

import java.awt.*;

public class GUI {
    private TrayIcon trayIcon;
    public GUI() {
        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/logo.jpg"));

        TrayIcon trayIcon = new TrayIcon(image, "GitHub Manager");
        this.trayIcon = trayIcon;
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("GitHub Manager");
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }
    public void sendMessage (String title, String text) {
        trayIcon.displayMessage(title, text, TrayIcon.MessageType.INFO);
    }
}
