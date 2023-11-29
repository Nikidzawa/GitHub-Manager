package ru.nikidzawa.github_manager.telegram.service;

import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.antlr.v4.runtime.misc.Pair;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
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
import ru.nikidzawa.github_manager.telegram.config.BotConfiguration;
import ru.nikidzawa.github_manager.telegram.store.entities.UserEntity;
import ru.nikidzawa.github_manager.telegram.store.repositories.UserRepository;

import java.util.*;
import java.util.List;

@Component
@SuppressWarnings("deprecation")
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    UserRepository repository;
    BotConfiguration config;

    public TelegramBot(BotConfiguration config) throws TelegramApiException {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Обновить токен"));
        listOfCommands.add(new BotCommand("/menu", "Главное меню"));
        listOfCommands.add(new BotCommand("/info", "Небольшая информация"));
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
    public HashSet<Long> wait2 = new HashSet<>();
    private HashMap<Long, GitHubData> gitHub = new HashMap<>();
    @SneakyThrows
    @Override
    @Transactional
    public void onUpdateReceived(Update update) {
        Long userId = update.hasCallbackQuery() ?
                update.getCallbackQuery().getMessage().getChatId() : update.getMessage().getChatId();

        GitHubData data = gitHub.getOrDefault(userId, new GitHubData());
        GitHubManager gitHubManager = data.getGitHubManager();
        Configuration configuration = data.getConfiguration();
        GitHub userGitHub = data.getGitHub();
        ResourceBundle messages = ResourceBundle.getBundle("messages", configuration.getSelectedLocale());

        Menu menu = new Menu(messages, userId,this);
        Settings settings = new Settings(userId, messages, this);

        UserEntity user = repository.findById(userId).orElseGet(() -> {
            UserEntity entity = new UserEntity();
            entity.setId(userId);
            repository.save(entity);
            return entity;
        });

        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            if (wait.contains(userId)) {
                if (gitHubManager != null) {
                    gitHubManager.stopSession();
                }
                sendMessage(userId, messages.getString("token_accept"));
                byte[] token;
                if (message.equals(messages.getString("use_last_token"))) {
                    token = user.getToken();
                } else {
                    token = Crypto.encryptData(message);
                    user.setToken(token);
                    repository.save(user);
                }
                gitHubManager = new GitHubManager(userId, token, configuration, this);
                userGitHub = gitHubManager.getGitHub();
                menu.startMenu(userGitHub, gitHubManager, configuration);
            } else if (wait2.contains(userId)) {
                if (message.equals(messages.getString("Back"))) {
                    wait2.remove(userId);
                    sendMessageInlineMarkup(userId, messages.getString("settings"),
                            inlineKeyBoardMarkupBuilder(settings.settingsButtons(configuration)));
                    return;
                }
                int time = 0;
                try {
                    time = Integer.parseInt(message);
                } catch (RuntimeException ex) {
                    sendMessageNotRemoveMarkup(userId, messages.getString("ex_timer"));
                }
                if (time <= 86400) {
                    wait2.remove(userId);
                    configuration.setExpirationTime(time);
                    gitHubManager.startSession();
                    sendMessageInlineMarkup(userId, messages.getString("settings"),
                            inlineKeyBoardMarkupBuilder(settings.settingsButtons(configuration)));
                }
                else {
                    sendMessageNotRemoveMarkup(userId, messages.getString("ex_timer"));
                }
            }
            else {
                if (message.equals(messages.getString("lets_go"))) {
                    wait.add(userId);
                    sendMessage(userId, messages.getString("message_get_token"));
                    return;
                }
                switch (message) {
                    case "/start":
                        if (user.getToken() == null) {
                            ReplyKeyboardMarkup start = keyBoardMarkupBuilder(List.of(messages.getString("lets_go")));
                            sendMessage(userId, messages.getString("start_hello"));
                            sendMessageReplyKeyboardMarkup(userId, messages.getString("start_hello2"), start);
                            break;
                        }
                        wait.add(userId);
                        List<String> getToken = List.of(messages.getString("use_last_token"));
                        ReplyKeyboardMarkup token = keyBoardMarkupBuilder(getToken);

                        sendMessageReplyKeyboardMarkup(userId, messages.getString("select_token"), token);
                        break;
                    case "/menu" :
                        if (user.getToken() != null) {
                            menu.startMenu(userGitHub, gitHubManager, configuration);
                            break;
                        }
                        sendMessage(userId, messages.getString("ex_token"));
                        break;
                    case "/info":
                        sendMessage(userId, messages.getString("info"));
                        break;
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            switch (callBackData) {
                case "MENU":
                    menu.startMenu(userGitHub, gitHubManager, configuration);
                    break;
                case "LANGUAGE":
                    configuration.setSelectedLocale(
                            new Locale(configuration.getSelectedLocale().getLanguage().equals("ru") ? "en" : "ru"));
                    settings.setSettings(configuration, messageId);
                    break;
                case "COMMITS":
                    configuration.setShowCommits(!configuration.isShowCommits());
                    settings.setSettings(configuration, messageId);
                    break;
                case "PULLS":
                    configuration.setShowPullRequests(!configuration.isShowPullRequests());
                    settings.setSettings(configuration, messageId);
                    break;
                case "STARS":
                    configuration.setShowStars(!configuration.isShowStars());
                    settings.setSettings(configuration, messageId);
                    break;
                case "CHANGE_SESSION_STATUS":
                    if (gitHubManager.sessionIsActive()) gitHubManager.stopSession();
                    else gitHubManager.startSession();
                    menu.changeMenu(messageId, userGitHub, gitHubManager, configuration);
                    break;
                case "SETTINGS_ACCEPT" :
                    gitHubManager.startSession();
                    menu.startMenu(userGitHub, gitHubManager, configuration);
                    break;
                case "SETTINGS" :
                    gitHubManager.stopSession();
                    sendMessageInlineMarkup(userId, messages.getString("settings"),
                            inlineKeyBoardMarkupBuilder(settings.settingsButtons(configuration)));
                    break;
                case "CHANGE_SHOWING_REPOS" :
                    configuration.setShowRepos(!configuration.isShowRepos());
                    menu.changeMenu(messageId, userGitHub, gitHubManager, configuration);
                    break;
                case "EXPIRATION_NOTIFICATION" :
                    sendMessageReplyKeyboardMarkup(userId, messages.getString("EXPIRATION_NOTIFICATION"),
                            keyBoardMarkupBuilder(List.of(messages.getString("Back"))));
                    wait2.add(userId);
            }
        }
        gitHub.remove(userId);
        gitHub.put(userId, new GitHubData(configuration, userGitHub, gitHubManager));
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
    public void sendMessageInlineMarkupAndDelete(Long userId, String message, InlineKeyboardMarkup markup, int seconds) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(markup);
        sendMessage.setChatId(userId);
        sendMessage.setText(message);
        Message mes = execute(sendMessage);
        if (seconds == 0) {
            return;
        }
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(userId);
                deleteMessage.setMessageId(mes.getMessageId());
                try {
                    execute(deleteMessage);
                } catch (TelegramApiException e) {
                    sendMessage(userId, "Не удалось удалить уведомление");
                }
            }
        }, seconds * 1000L);
    }
    @SneakyThrows
    public void sendMessageInlineMarkup(Long userId, String message, InlineKeyboardMarkup markup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(markup);
        sendMessage.setChatId(userId);
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
        sendMessage.setParseMode("Markdown");
        sendMessage.setText(message);
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove(true);
        sendMessage.setReplyMarkup(replyKeyboardRemove);
        execute(sendMessage);
    }
    @SneakyThrows
    public void sendMessageNotRemoveMarkup (Long id, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(id);
        sendMessage.setParseMode("Markdown");
        sendMessage.setText(message);
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove(true);
        sendMessage.setReplyMarkup(replyKeyboardRemove);
        execute(sendMessage);
    }
}

