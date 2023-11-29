package ru.nikidzawa.github_manager.telegram.service;

import lombok.SneakyThrows;
import org.kohsuke.github.GitHub;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nikidzawa.github_manager.Configuration;
import ru.nikidzawa.github_manager.GitHubManager;
import ru.nikidzawa.github_manager.RepositoryDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Menu {
    Long userId;
    TelegramBot telegramBot;
    ResourceBundle messages;
    public Menu (ResourceBundle messages, long userId, TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
        this.messages = messages;
        this.userId = userId;
    }
    @SneakyThrows
    public void startMenu (GitHub userGitHub, GitHubManager gitHubManager, Configuration configuration) {
        String photoUrl = userGitHub.getMyself().getAvatarUrl();
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(userId);
        sendPhoto.setCaption(menuCaption(gitHubManager, userGitHub));
        sendPhoto.setReplyMarkup(menuButtons(userGitHub, gitHubManager, configuration));
        sendPhoto.setPhoto(new InputFile(photoUrl));
        telegramBot.execute(sendPhoto);
    }
    @SneakyThrows
    public void changeMenu(long messageID, GitHub userGitHub, GitHubManager gitHubManager, Configuration configuration) {
        EditMessageCaption messageCaption = new EditMessageCaption();
        messageCaption.setChatId(userId);
        messageCaption.setChatId(userId);
        messageCaption.setMessageId((int) messageID);
        messageCaption.setParseMode("Markdown");
        if (configuration.isShowRepos()) {
            messageCaption.setCaption(menuCaptionAndRepos(gitHubManager, userGitHub));
        } else {
            messageCaption.setCaption(menuCaption(gitHubManager, userGitHub));
        }
        messageCaption.setReplyMarkup(menuButtons(userGitHub, gitHubManager, configuration));

        telegramBot.execute(messageCaption);
    }
    @SneakyThrows
    private InlineKeyboardMarkup menuButtons (GitHub userGitHub, GitHubManager gitHubManager, Configuration configuration) {
        List<List<InlineKeyboardButton>> rowInline = new ArrayList<>();

        List<InlineKeyboardButton> accountRow = new ArrayList<>();
        InlineKeyboardButton account = new InlineKeyboardButton();
        account.setText(userGitHub.getMyself().getName());
        account.setUrl(userGitHub.getMyself().getHtmlUrl().toString());

        accountRow.add(account);

        List<InlineKeyboardButton> session = new ArrayList<>();
        InlineKeyboardButton sessionButton = new InlineKeyboardButton();
        sessionButton.setText(getSessionStatusButton(gitHubManager.sessionIsActive()));
        sessionButton.setCallbackData("CHANGE_SESSION_STATUS");
        session.add(sessionButton);

        List<InlineKeyboardButton> notifications = new ArrayList<>();
        InlineKeyboardButton notificationsButton = new InlineKeyboardButton();
        notificationsButton.setText(messages.getString("notifications"));
        notificationsButton.setUrl("https://github.com/notifications");
        notifications.add(notificationsButton);

        List<InlineKeyboardButton> repos = new ArrayList<>();
        InlineKeyboardButton reposButton = new InlineKeyboardButton();
        reposButton.setText(showRepos(configuration.isShowRepos()));
        reposButton.setCallbackData("CHANGE_SHOWING_REPOS");
        repos.add(reposButton);

        List<InlineKeyboardButton> settings = new ArrayList<>();
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText(messages.getString("settings"));
        settingsButton.setCallbackData("SETTINGS");
        settings.add(settingsButton);

        rowInline.add(accountRow);
        rowInline.add(notifications);
        rowInline.add(repos);
        rowInline.add(session);
        rowInline.add(settings);

        return new InlineKeyboardMarkup(rowInline);
    }
    private String showRepos (boolean status) {
        return status ? messages.getString("repos_hide") : messages.getString("repos_show");
    }
    private String getSessionStatusButton (boolean sessionIsActive) {
        return sessionIsActive ? messages.getString("session_STOP") : messages.getString("session_START");
    }
    @SneakyThrows
    private String menuCaption (GitHubManager gitHubManager, GitHub userGitHub) {
        return messages.getString("main_menu") + messages.getString("account") +
                userGitHub.getMyself().getName() + messages.getString("session_status") +
                getSessionStatus(gitHubManager.sessionIsActive());
    }
    private String getSessionStatus(boolean sessionIsActive) {
        return sessionIsActive ? messages.getString("session_ON") : messages.getString("session_OFF");
    }
    @SneakyThrows
    private String menuCaptionAndRepos (GitHubManager gitHubManager, GitHub userGitHub) {
        return messages.getString("main_menu") + messages.getString("account") +
                userGitHub.getMyself().getName() + messages.getString("session_status") +
                getSessionStatus(gitHubManager.sessionIsActive()) + "\n\n" + getReposInfo(gitHubManager);
    }
    private String getReposInfo(GitHubManager gitHubManager) {
        StringBuilder stringBuilder = new StringBuilder();
        for (RepositoryDescription repos : gitHubManager.getRepos()) {
            stringBuilder.append("\uD83D\uDCD9 [").append(repos.getName()).append("](").append(repos.getGhRepository().getHtmlUrl())
                    .append(")\n")
                    .append("⭐ ").append(repos.getStarsCount()).append("\n")
                    .append("[").append(messages.getString("commits2")).append("](https://github.com/")
                    .append(repos.getGhRepository().getFullName()).append("/commits/main) ").append(repos.getCommits().size())
                    .append("\n")
                    .append("[").append(messages.getString("pulls2")).append("](https://github.com/")
                    .append(repos.getGhRepository().getFullName()).append("/pulls) ").append(repos.getPullRequests().size())
                    .append("\n\n");
        }

        return stringBuilder.toString();
    }
}
