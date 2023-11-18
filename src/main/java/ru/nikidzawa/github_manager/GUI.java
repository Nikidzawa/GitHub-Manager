package ru.nikidzawa.github_manager;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

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
    public void setMenu (String login, List<RepositoryDescription> repository) {
        PopupMenu popup = new PopupMenu();

        MenuItem accountMI = new MenuItem(login);
        accountMI.addActionListener(e -> openInBrowser("https://github.com/" + login));

        MenuItem notificationMI = new MenuItem("notifications");
        notificationMI.addActionListener(e -> openInBrowser("https://github.com/notifications"));

        Menu repositoriesMI = new Menu("repositories");
        repository.forEach(repos -> {
            Menu repoSM = new Menu(repos.getGhRepository().getName());

            MenuItem openInBrowser = new MenuItem("Open in browser");
            openInBrowser.addActionListener(e -> openInBrowser(repos.getGhRepository().getHtmlUrl().toString()));

            repoSM.add(openInBrowser);

            if (!repos.getPrs().isEmpty()) {
                repoSM.addSeparator();
            }

            repos.getPrs().forEach(pr -> {
                MenuItem prMi = new MenuItem(pr.getTitle());
                prMi.addActionListener(e -> openInBrowser(pr.getHtmlUrl().toString()));
                repoSM.add(prMi);
            });

            repositoriesMI.add(repoSM);
        });

        popup.add(accountMI);
        popup.addSeparator();
        popup.add(notificationMI);
        popup.add(repositoriesMI);


        trayIcon.setPopupMenu(popup);
    }

    private void openInBrowser(String url) {
        try {
            Desktop.getDesktop().browse(new URL(url).toURI());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    public void sendMessage (String title, String text) {
        trayIcon.displayMessage(title, text, TrayIcon.MessageType.INFO);
    }
}
