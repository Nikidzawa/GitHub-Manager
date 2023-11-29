package ru.nikidzawa.github_manager.telegram.service;

import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.nikidzawa.github_manager.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class Settings {
    Long userId;
    TelegramBot telegramBot;
    ResourceBundle messages;
    public Settings (Long userId, ResourceBundle messages, TelegramBot telegramBot) {
        this.userId = userId;
        this.telegramBot = telegramBot;
        this.messages = messages;
    }
    @SneakyThrows
    public void setSettings (Configuration configuration, long messageId) {
        EditMessageText SETTINGS = new EditMessageText();
        SETTINGS.setChatId(userId);
        SETTINGS.setText(messages.getString("settings"));
        InlineKeyboardMarkup updatedSettingsMarkup = telegramBot.inlineKeyBoardMarkupBuilder(settingsButtons(configuration));
        SETTINGS.setReplyMarkup(updatedSettingsMarkup);
        SETTINGS.setMessageId((int) messageId);
        telegramBot.execute(SETTINGS);
    }
    public List<List<InlineButtonInfo>> settingsButtons (Configuration configuration) {
        List<List<InlineButtonInfo>> settings = new ArrayList<>();
        List<InlineButtonInfo> country = new ArrayList<>();
        country.add(new InlineButtonInfo(messages.getString("select_language_TG") +
                flag(configuration.getSelectedLocale()), "LANGUAGE"));

        List<InlineButtonInfo> settingsButtons = new ArrayList<>();
        settingsButtons.add(new InlineButtonInfo(messages.getString("commits_TG") +
                onOrOff(configuration.isShowCommits()), "COMMITS"));
        settingsButtons.add(new InlineButtonInfo(messages.getString("pull_requests_TG")+
                onOrOff(configuration.isShowPullRequests()), "PULLS"));
        settingsButtons.add(new InlineButtonInfo(messages.getString("stars_TG") +
                onOrOff(configuration.isShowStars()), "STARS"));

        List<InlineButtonInfo> expirationTimer = new ArrayList<>();
        expirationTimer.add(new InlineButtonInfo(getExpirationTimerText(configuration), "EXPIRATION_NOTIFICATION"));

        List<InlineButtonInfo> accept = new ArrayList<>();
        accept.add(new InlineButtonInfo(messages.getString("SETTINGS_ACCEPT"), "SETTINGS_ACCEPT"));

        settings.add(country);
        settings.add(settingsButtons);
        settings.add(expirationTimer);
        settings.add(accept);
        return settings;
    }
    private String getExpirationTimerText (Configuration configuration) {
        return configuration.getExpirationTime() == 0? messages.getString("expiration_OFF") :
                messages.getString("expiration_ON") + configuration.getExpirationTime() + messages.getString("sec");
    }
    private String onOrOff (boolean confInfo) {
        return confInfo ? "✅" : "❌";
    }
    private String flag (Locale locale) {
        return locale.getLanguage().equals("ru") ? "\uD83C\uDDF7\uD83C\uDDFA" : "\uD83C\uDDEC\uD83C\uDDE7";
    }
}
