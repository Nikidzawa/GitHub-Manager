package ru.nikidzawa.github_manager.telegram.service;

import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.antlr.v4.runtime.misc.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.nikidzawa.github_manager.desktop.Configuration;
import ru.nikidzawa.github_manager.desktop.GitHubManager;
import ru.nikidzawa.github_manager.telegram.config.BotConfiguration;
import ru.nikidzawa.github_manager.telegram.store.entities.UserEntity;
import ru.nikidzawa.github_manager.telegram.store.repositories.UserRepository;

import java.util.*;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    UserRepository repository;
    BotConfiguration config;

    public TelegramBot(BotConfiguration config) throws TelegramApiException {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Начать сессию"));
        listOfCommands.add(new BotCommand("/settings", "Настройки бота"));
        this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
    }
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
    @Override
    public String getBotToken() {
        return config.getToken();
    }
    public HashSet<Long> wait = new HashSet<>();
    private HashMap<Long, Pair<GitHubManager, Configuration>> gitHub = new HashMap<>();
    @SneakyThrows
    @Override
    @Transactional
    public void onUpdateReceived(Update update) {
        Long userId = update.hasCallbackQuery() ?
                update.getCallbackQuery().getMessage().getChatId() : update.getMessage().getChatId();

        Pair<GitHubManager, Configuration> pair = gitHub.getOrDefault(userId, new Pair<>(null, null));
        GitHubManager gitHubManager = pair.a;
        Configuration configuration = pair.b;

        UserEntity user = repository.findById(userId).orElseGet(() -> {
            UserEntity entity = new UserEntity();
            entity.setId(userId);
            repository.save(entity);
            return entity;
        });
        if (gitHubManager != null) {
            gitHub.remove(userId);
            gitHubManager.stopTimer();
        }

            if (update.hasMessage() && update.getMessage().hasText()) {
                String message = update.getMessage().getText();
                if (wait.contains(userId)) {
                    sendMessage(userId, "Токен был получен. Ожидание ответа от Github Api ");
                    if (configuration == null) {
                        configuration = new Configuration();
                    }

                    if (message.equals("Использовать предыдущий токен")) {
                        gitHubManager = new GitHubManager(userId, user.getToken(), configuration, this);
                    }
                    else {
                        user.setToken(message);
                        repository.save(user);
                        gitHubManager = new GitHubManager(userId, message, configuration, this);
                    }
                }

                else {
                    switch (message) {
                        case "/start":
                            wait.add(userId);
                            if (user.getToken() != null) {
                                List<String> getToken = List.of("Использовать предыдущий токен");
                                ReplyKeyboardMarkup token = keyBoardMarkupBuilder(getToken);
                                sendMessageReplyKeyboardMarkup(userId,
                                        "Ваша сессия завершена. Для открытия новой сессии, вам необходимо указать токен",
                                        token);
                                break;
                            }
                            sendMessageInlineMarkup(userId, "Я ваш персональный GitHub помощник, " +
                                    "который будет уведомлять вас об изменениях в репозиториях\n" +
                                    "Для продолженя, введите ваш GitHub токен", null);
                            break;
                        case "/settings":
                            List<InlineButtonInfo> settings = new ArrayList<>();
                            settings.add(new InlineButtonInfo("Язык " + configuration.getSelectedLocale(), "LANGUAGE"));
                            settings.add(new InlineButtonInfo("Коммиты " + configuration.isShowCommits(), "COMMITS"));
                            settings.add(new InlineButtonInfo("Пулл реквесты " + configuration.isShowPullRequests(), "PULLS"));
                            settings.add(new InlineButtonInfo("Звёзды " + configuration.isShowStars(), "STARS"));
                            InlineKeyboardMarkup settingsMarkup = inlineKeyBoardMarkupBuilder(settings);
                            sendMessageInlineMarkup(userId, "Настройки", settingsMarkup);
                            break;
                        case "/info":

                    }
                }
            }
            else if (update.hasCallbackQuery()) {
                String callBackData = update.getCallbackQuery().getData();
                long messageId = update.getCallbackQuery().getMessage().getMessageId();
                switch (callBackData) {
                    case "LANGUAGE":
                        configuration.setSelectedLocale(configuration.getSelectedLocale().getLanguage().equals("ru") ?
                                new Locale("en") : new Locale("ru"));
                        setSettings(configuration, userId, messageId);
                        break;
                    case "COMMITS":
                        configuration.setShowCommits(!configuration.isShowCommits());
                        setSettings(configuration, userId, messageId);
                        break;
                    case "PULLS":
                        configuration.setShowPullRequests(!configuration.isShowPullRequests());
                        setSettings(configuration, userId, messageId);
                        break;
                    case "STARS" :
                        configuration.setShowStars(!configuration.isShowStars());
                        setSettings(configuration, userId, messageId);
                }
            }
            gitHub.remove(userId);
            pair = new Pair<>(gitHubManager, configuration);
            gitHub.put(userId, pair);
    }

    public InlineKeyboardMarkup inlineKeyBoardMarkupBuilder(List<InlineButtonInfo> buttonInfoList) {
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (InlineButtonInfo buttonInfo : buttonInfoList) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonInfo.getText());
            button.setCallbackData(buttonInfo.getCallbackData());

            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(button);
            rowsInline.add(rowInline);
        }
        return new InlineKeyboardMarkup(rowsInline);
    }
    public InlineKeyboardMarkup urlIneKeyBoardMarkupBuilder (String text, String url) {
        InlineKeyboardButton repositoryButton = new InlineKeyboardButton();
        repositoryButton.setText(text);
        repositoryButton.setUrl(url);

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(Collections.singletonList(repositoryButton));
        return new InlineKeyboardMarkup(buttons);
    }
    public ReplyKeyboardMarkup keyBoardMarkupBuilder(List<String> buttonLabels) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        for (String label : buttonLabels) {
            keyboardRow.add(label);
        }
        keyboardRows.add(keyboardRow);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }
    @SneakyThrows
    private void setSettings (Configuration configuration, Long userId, long messageId) {
        EditMessageText SETTINGS = new EditMessageText();
        SETTINGS.setChatId(userId);
        SETTINGS.setText("Настройки");
        List<InlineButtonInfo> updatedSettings = new ArrayList<>();
        updatedSettings.add(new InlineButtonInfo("Язык " + configuration.getSelectedLocale(), "LANGUAGE"));
        updatedSettings.add(new InlineButtonInfo("Коммиты " + configuration.isShowCommits(), "COMMITS"));
        updatedSettings.add(new InlineButtonInfo("Пулл реквесты " + configuration.isShowPullRequests(), "PULLS"));
        updatedSettings.add(new InlineButtonInfo("Звёзды " + configuration.isShowStars(), "STARS"));
        InlineKeyboardMarkup updatedSettingsMarkup = inlineKeyBoardMarkupBuilder(updatedSettings);
        SETTINGS.setReplyMarkup(updatedSettingsMarkup);
        SETTINGS.setMessageId((int) messageId);
        execute(SETTINGS);
    }

    @SneakyThrows
    public void sendMessageInlineMarkup(Long id, String message, InlineKeyboardMarkup markup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(markup);
        sendMessage.setChatId(id);
        sendMessage.setText(message);
        execute(sendMessage);
    }
    @SneakyThrows
    public void sendMessageReplyKeyboardMarkup (Long id, String message, ReplyKeyboardMarkup markup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(id);
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(markup);
        execute(sendMessage);
    }
    @SneakyThrows
    public void sendMessage (Long id, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(id);
        sendMessage.setText(message);
        execute(sendMessage);
    }
}

