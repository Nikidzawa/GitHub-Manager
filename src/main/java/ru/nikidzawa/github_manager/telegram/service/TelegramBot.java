package ru.nikidzawa.github_manager.telegram.service;

import com.vdurmont.emoji.EmojiParser;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.nikidzawa.github_manager.Configuration;
import ru.nikidzawa.github_manager.GitHubManager;
import ru.nikidzawa.github_manager.telegram.Crypto;
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
        listOfCommands.add(new BotCommand("/start", "Начать новую сессию"));
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
                    sendMessage(userId, "Токен был получен. Ожидание ответа от Github Api");
                    if (configuration == null) {
                        configuration = new Configuration();
                    }
                    byte[] token;
                    if (message.equals("Использовать предыдущий токен")) {
                        token = user.getToken();
                    } else {
                        token = Crypto.encryptData(message);
                        user.setToken(token);
                        repository.save(user);
                    }

                    gitHubManager = new GitHubManager(userId, token, configuration, this);
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
                            sendMessage(userId,"\uD83E\uDD16 Я ваш персональный GitHub помощник, " +
                                    "который будет уведомлять вас об изменениях в репозиториях\n" +
                                    "Для продолженя, введите ваш GitHub токен");
                            break;
                        case "/settings":
                            InlineKeyboardMarkup settingsMarkup = inlineKeyBoardMarkupBuilder(settingsButtons(configuration));
                            sendMessageInlineMarkup(userId, "Настройки", settingsMarkup);
                            break;
                    }
                }
            }
            else if (update.hasCallbackQuery()) {
                String callBackData = update.getCallbackQuery().getData();
                long messageId = update.getCallbackQuery().getMessage().getMessageId();
                switch (callBackData) {
                    case "LANGUAGE" :
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
                        break;
                    case "SETTINGS_ACCEPT" :
                        gitHubManager = new GitHubManager(userId, user.getToken(), configuration, this);
                        break;
                }
            }
            gitHub.remove(userId);
            pair = new Pair<>(gitHubManager, configuration);
            gitHub.put(userId, pair);
    }

    public InlineKeyboardMarkup inlineKeyBoardMarkupBuilder(List<List<InlineButtonInfo>> buttonInfoList) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttonInfoList.forEach(inlineButtonInfos -> {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            inlineButtonInfos.forEach(inlineButtonInfo -> {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(inlineButtonInfo.getText());
                button.setCallbackData(inlineButtonInfo.getCallbackData());

                rowInline.add(button);
            });
            buttons.add(rowInline);
        });
        return new InlineKeyboardMarkup(buttons);
    }
    public InlineKeyboardMarkup urlIneKeyBoardMarkupBuilder (List<Pair<String, String>> pairs) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        pairs.forEach(pair -> {
            InlineKeyboardButton repositoryButton = new InlineKeyboardButton();
            repositoryButton.setText(pair.a);
            repositoryButton.setUrl(pair.b);
            rowInline.add(repositoryButton);
        });
        buttons.add(rowInline);
        return new InlineKeyboardMarkup(buttons);
    }
    public InlineKeyboardMarkup urlIneKeyBoardMarkupBuilder(String text, String url) {
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
        InlineKeyboardMarkup updatedSettingsMarkup = inlineKeyBoardMarkupBuilder(settingsButtons(configuration));
        SETTINGS.setReplyMarkup(updatedSettingsMarkup);
        SETTINGS.setMessageId((int) messageId);
        execute(SETTINGS);
    }
    private List<List<InlineButtonInfo>> settingsButtons (Configuration configuration) {
        List<List<InlineButtonInfo>> settings = new ArrayList<>();
        List<InlineButtonInfo> country = new ArrayList<>();
        country.add(new InlineButtonInfo("Язык " + flag(configuration.getSelectedLocale()), "LANGUAGE"));

        List<InlineButtonInfo> settingsButtons = new ArrayList<>();
        settingsButtons.add(new InlineButtonInfo("Коммиты " + onOrOff(configuration.isShowCommits()), "COMMITS"));
        settingsButtons.add(new InlineButtonInfo("Реквесты " + onOrOff(configuration.isShowPullRequests()), "PULLS"));
        settingsButtons.add(new InlineButtonInfo("Звёзды " + onOrOff(configuration.isShowStars()), "STARS"));

        List<InlineButtonInfo> accept = new ArrayList<>();
        accept.add(new InlineButtonInfo("Применить настройки", "SETTINGS_ACCEPT"));

        settings.add(country);
        settings.add(settingsButtons);
        settings.add(accept);
        return settings;
    }
    private String onOrOff (boolean confInfo) {
        return confInfo? "✅" : "❌";
    }
    private String flag (Locale locale) {
        return locale.getLanguage().equals("ru") ? EmojiParser.parseToUnicode("\uD83C\uDDF7\uD83C\uDDFA") : EmojiParser.parseToUnicode("\uD83C\uDDEC\uD83C\uDDE7");
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
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove(true);
        sendMessage.setReplyMarkup(replyKeyboardRemove);
        execute(sendMessage);
    }
}

