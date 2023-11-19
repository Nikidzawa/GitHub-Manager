package ru.nikidzawa.github_manager;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@SuppressWarnings("deprecation")
public class GUI {
    private TrayIcon trayIcon;
    private ResourceBundle messages;
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
        PopupMenu popupMenu = new PopupMenu();
        messages = ResourceBundle.getBundle("messages", Configuration.selectedLocale);

        MenuItem accountMI = new MenuItem(login);
        accountMI.addActionListener(e -> openInBrowser("https://github.com/" + login));

        MenuItem notificationMI = new MenuItem(messages.getString("notifications"));
        notificationMI.addActionListener(e -> openInBrowser("https://github.com/notifications"));

        Menu repositoriesMI = new Menu(messages.getString("repositories"));
        repository.forEach(repos -> {
            Menu repoSM = new Menu(repos.getName());

            MenuItem openInBrowser = new MenuItem(messages.getString("open_in_browser"));
            openInBrowser.addActionListener(e -> openInBrowser(repos.getGhRepository().getHtmlUrl().toString()));

            repoSM.add(openInBrowser);

            if (!repos.getPullRequests().isEmpty()) {repoSM.addSeparator();}

            Menu pullRequest = new Menu(messages.getString("pull_requests"));
            repoSM.add(pullRequest);
            Menu commits = new Menu(messages.getString("commits"));
            repoSM.add(commits);

            repos.getPullRequests().forEach(pr -> {
                MenuItem prMi = new MenuItem(pr.getTitle());
                prMi.addActionListener(e -> openInBrowser(pr.getHtmlUrl().toString()));
                pullRequest.add(prMi);
            });
            repos.getCommits().forEach(ghCommit -> {
                try {
                    Date date = ghCommit.getCommitDate();
                    MenuItem commitMenuItem = new MenuItem(ghCommit.getCommitShortInfo().getMessage() + "    " +
                            date.getDay() + "." + date.getMonth());
                    commitMenuItem.addActionListener(e -> openInBrowser(ghCommit.getHtmlUrl().toString()));
                    commits.add(commitMenuItem);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            repositoriesMI.add(repoSM);
        });
        popupMenu.add(accountMI);
        popupMenu.addSeparator();
        popupMenu.add(notificationMI);
        popupMenu.add(repositoriesMI);
        popupMenu.add(settings());

        trayIcon.setPopupMenu(popupMenu);

    }

    private Menu settings() {
        Menu settingsMI = new Menu(messages.getString("settings"));

        Menu LanguagesMI = new Menu(messages.getString("select_language"));
        CheckboxMenuItem russLang;
        CheckboxMenuItem engLang;
        if (Configuration.selectedLocale.equals(new Locale("ru"))) {
            russLang = new CheckboxMenuItem("Russian", true);
            engLang = new CheckboxMenuItem("English", false);
        }
        else {
            russLang = new CheckboxMenuItem("Russian", false);
            engLang = new CheckboxMenuItem("English", true);
        }
        russLang.addItemListener(e -> Configuration.selectedLocale = new Locale("ru"));
        engLang.addItemListener(e -> Configuration.selectedLocale = new Locale("en"));

        Menu messagesConfMI = new Menu(messages.getString("messages_settings"));

        CheckboxMenuItem commitsConf = new CheckboxMenuItem(messages.getString("commits"),
                Configuration.showCommits);
        commitsConf.addItemListener(e -> Configuration.showCommits = !Configuration.showCommits);

        CheckboxMenuItem pullsConf = new CheckboxMenuItem(messages.getString("pull_requests"),
                Configuration.showPullRequests);
        pullsConf.addItemListener(e -> Configuration.showPullRequests = !Configuration.showPullRequests);

        CheckboxMenuItem starConf = new CheckboxMenuItem(messages.getString("stars"),
                Configuration.showStars);
        starConf.addItemListener(e -> Configuration.showStars = !Configuration.showStars);

        messagesConfMI.add(commitsConf);
        messagesConfMI.add(pullsConf);
        messagesConfMI.add(starConf);

        LanguagesMI.add(russLang);
        LanguagesMI.add(engLang);

        settingsMI.add(LanguagesMI);
        settingsMI.add(messagesConfMI);
        return settingsMI;
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
