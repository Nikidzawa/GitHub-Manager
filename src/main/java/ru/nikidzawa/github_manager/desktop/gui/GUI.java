package ru.nikidzawa.github_manager.desktop.gui;

import ru.nikidzawa.github_manager.Configuration;
import ru.nikidzawa.github_manager.RepositoryDescription;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@SuppressWarnings("deprecation")
public class GUI {
    private TrayIcon trayIcon;
    private ResourceBundle messages;
    private Configuration configuration;
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
    public void setMenu (String login, List<RepositoryDescription> repository, Configuration configuration) {
        this.configuration = configuration;
        PopupMenu popupMenu = new PopupMenu();
        messages = ResourceBundle.getBundle("messages", configuration.getSelectedLocale());

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
                try {
                    MenuItem prMi = new MenuItem(pr.getTitle() + getDate(pr.getCreatedAt()));
                    prMi.addActionListener(e -> openInBrowser(pr.getHtmlUrl().toString()));
                    pullRequest.add(prMi);
                }catch (IOException ex) {
                    throw new RuntimeException();
                }
            });
            repos.getCommits().forEach(ghCommit -> {
                try {
                    MenuItem commitMenuItem = new MenuItem(
                            ghCommit.getCommitShortInfo().getMessage() + getDate(ghCommit.getCommitDate()));
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
        if (configuration.getSelectedLocale().equals(new Locale("ru"))) {
            russLang = new CheckboxMenuItem("Russian", true);
            engLang = new CheckboxMenuItem("English", false);
        }
        else {
            russLang = new CheckboxMenuItem("Russian", false);
            engLang = new CheckboxMenuItem("English", true);
        }
        russLang.addItemListener(e -> configuration.setSelectedLocale(new Locale("ru")));
        engLang.addItemListener(e -> configuration.setSelectedLocale(new Locale("en")));

        Menu messagesConfMI = new Menu(messages.getString("messages_settings"));

        CheckboxMenuItem commitsConf = new CheckboxMenuItem(messages.getString("commits"),
                configuration.isShowCommits());
        commitsConf.addItemListener(e -> configuration.setShowCommits(!configuration.isShowCommits()));

        CheckboxMenuItem pullsConf = new CheckboxMenuItem(messages.getString("pull_requests"),
                configuration.isShowPullRequests());
        pullsConf.addItemListener(e -> configuration.setShowPullRequests(!configuration.isShowPullRequests()));

        CheckboxMenuItem starConf = new CheckboxMenuItem(messages.getString("stars"),
                configuration.isShowStars());
        starConf.addItemListener(e -> configuration.setShowStars(!configuration.isShowStars()));

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
    private String getDate (Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        return "   " + messages.getString("date") + dateFormat.format(date);
    }
}
