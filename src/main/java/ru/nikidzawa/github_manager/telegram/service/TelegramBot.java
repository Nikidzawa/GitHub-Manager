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
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
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
import ru.nikidzawa.github_manager.RepositoryDescription;
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
                sendMessage(userId, "Токен был получен. Ожидание ответа от GitHub Api");
                byte[] token;
                if (message.equals("Использовать предыдущий токен")) {
                    token = user.getToken();
                } else {
                    token = Crypto.encryptData(message);
                    user.setToken(token);
                    repository.save(user);
                }
                gitHubManager = new GitHubManager(userId, token, configuration, this);
                userGitHub = gitHubManager.getGitHub();
                startMenu(userGitHub, gitHubManager, userId, configuration);
            } else if (wait2.contains(userId)) {
                int time = 0;
                try {
                    time = Integer.parseInt(message);
                } catch (RuntimeException ex) {
                    sendMessage(userId, "Необходимо указать просто число, без букв");
                }
                if (Integer.parseInt(message) <= 86400) {
                    wait2.remove(userId);
                    configuration.setExpirationTime(time);
                    gitHubManager.startSession();
                    startMenu(userGitHub, gitHubManager, userId, configuration);
                }
                else {
                    sendMessage(userId, "Время должно быть меньше 86400");
                }
            }
            else {
                switch (message) {
                    case "/start":
                        if (user.getToken() == null) {
                            ReplyKeyboardMarkup start = keyBoardMarkupBuilder(List.of("Приступим!"));
                            sendMessage(userId, "\uD83E\uDD16 Здравствуйте! Я ваш персональный GitHub помощник");
                            sendMessageReplyKeyboardMarkup(userId,
                                    """
                                            вот мои функции \uD83D\uDCA1
                                            
                                            - Уведомлю вас об изменениях в репозиториях.
                                            - Покажу краткую информацию о изменениях и предоставлю быстрый доступ ко всем необходимым ссылкам.
                                            - Дам краткий отчет по репозиториям, прикреплю ссылки которые вам могут понадобится.
                                            
                                            Готовы начать?""", start);
                            break;
                        }
                        wait.add(userId);
                        List<String> getToken = List.of("Использовать предыдущий токен");
                        ReplyKeyboardMarkup token = keyBoardMarkupBuilder(getToken);

                        sendMessageReplyKeyboardMarkup(userId, "Укажите новый токен", token);
                        break;
                    case "Приступим!" :
                        wait.add(userId);
                        sendMessage(userId, "Для начала, мне нужно получить ваш GitHub токен");
                        break;
                    case "/menu" :
                        if (user.getToken() != null) {
                            startMenu(userGitHub, gitHubManager, userId, configuration);
                            break;
                        }
                        sendMessage(userId, "Чтобы получить доступ к функциям приложения, я должен знать ваш токен. Для этого, введите /start и следуйте инструкции");
                        break;
                    case "/info":
                        sendMessage(userId,
                                """
                                        \uD83D\uDD04 /start обновить токен

                                        \uD83C\uDFE0 /menu перейти в главное меню

                                        \uD83D\uDCAC Отзыв можете оставить [у меня](https://t.me/Nikidzawa) в личных сообщениях

                                        \uD83C\uDF10 Десктопную версию можете найти в моём [GitHub](https://github.com/Nikidzawa/GitHub-Manager)

                                        Приятного пользоавния! ✨""");
                        break;
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            switch (callBackData) {
                case "MENU":
                    startMenu(userGitHub, gitHubManager, userId, configuration);
                    break;
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
                case "STARS":
                    configuration.setShowStars(!configuration.isShowStars());
                    setSettings(configuration, userId, messageId);
                    break;
                case "CHANGE_SESSION_STATUS":
                    if (gitHubManager.sessionIsActive()) {
                        gitHubManager.stopSession();
                    }
                    else {
                        gitHubManager.startSession();
                    }
                    changeMenu(userId, messageId, userGitHub, gitHubManager, configuration);
                    break;
                case "SETTINGS_ACCEPT" :
                    gitHubManager.startSession();
                    startMenu(userGitHub, gitHubManager, userId, configuration);
                    break;
                case "SETTINGS" :
                    gitHubManager.stopSession();
                    sendMessageInlineMarkup(userId, "Настройки", inlineKeyBoardMarkupBuilder(settingsButtons(configuration)));
                    break;
                case "CHANGE_SHOWING_REPOS" :
                    configuration.setShowRepos(!configuration.isShowRepos());
                    changeMenu(userId, messageId, userGitHub, gitHubManager, configuration);
                    break;
                case "EXPIRATION_NOTIFICATION" :
                    sendMessage(userId, "Введите продолжительность жизни уведомлений в секундах, но не более 86400 (сутки)");
                    wait2.add(userId);
            }
        }
        gitHub.remove(userId);
        gitHub.put(userId, new GitHubData(configuration, userGitHub, gitHubManager));
    }
    @SneakyThrows
    private void changeMenu(Long userId, long messageID, GitHub userGitHub, GitHubManager gitHubManager, Configuration configuration) {
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

        execute(messageCaption);
    }

    @SneakyThrows
    private void startMenu (GitHub userGitHub, GitHubManager gitHubManager, Long userId, Configuration configuration) {
        String photoUrl = userGitHub.getMyself().getAvatarUrl();
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(userId);
        sendPhoto.setCaption(menuCaption(gitHubManager, userGitHub));
        sendPhoto.setReplyMarkup(menuButtons(userGitHub, gitHubManager, configuration));
        sendPhoto.setPhoto(new InputFile(photoUrl));
        execute(sendPhoto);
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
        notificationsButton.setText("Все уведомления");
        notificationsButton.setUrl("https://github.com/notifications");
        notifications.add(notificationsButton);

        List<InlineKeyboardButton> repos = new ArrayList<>();
        InlineKeyboardButton reposButton = new InlineKeyboardButton();
        reposButton.setText(showRepos(configuration.isShowRepos()));
        reposButton.setCallbackData("CHANGE_SHOWING_REPOS");
        repos.add(reposButton);

        List<InlineKeyboardButton> settings = new ArrayList<>();
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText("Настройки");
        settingsButton.setCallbackData("SETTINGS");
        settings.add(settingsButton);

        rowInline.add(accountRow);
        rowInline.add(notifications);
        rowInline.add(repos);
        rowInline.add(session);
        rowInline.add(settings);

        return new InlineKeyboardMarkup(rowInline);
    }
    @SneakyThrows
    private String menuCaption (GitHubManager gitHubManager, GitHub userGitHub) {
        return  "Главное меню" +
                "\n\nАккаунт: " + userGitHub.getMyself().getName() +
                "\nСтатус сессии: " + getSessionStatus(gitHubManager.sessionIsActive());
    }
    @SneakyThrows
    private String menuCaptionAndRepos (GitHubManager gitHubManager, GitHub userGitHub) {
            return "Главное меню" +
                    "\n\nАккаунт: " + userGitHub.getMyself().getName() +
                    "\nСтатус сессии: " + getSessionStatus(gitHubManager.sessionIsActive()) + "\n\n" + getReposInfo(gitHubManager);
    }
    private String getReposInfo(GitHubManager gitHubManager) {
        StringBuilder stringBuilder = new StringBuilder();
        for (RepositoryDescription repos : gitHubManager.getRepos()) {
            stringBuilder.append("\uD83D\uDCD9 [").append(repos.getName()).append("](").append(repos.getGhRepository().getHtmlUrl()).append(")\n")
                    .append("⭐ ").append(repos.getStarsCount()).append("\n")
                    .append("[Коммиты:](https://github.com/").append(repos.getGhRepository().getFullName()).append("/commits/main) ").append(repos.getCommits().size()).append("\n")
                    .append("[Пулл Реквесты:](https://github.com/").append(repos.getGhRepository().getFullName()).append("/pulls) ").append(repos.getPullRequests().size()).append("\n\n");
        }

        return stringBuilder.toString();
    }

    private String showRepos (boolean status) {
        return status ? "Скрыть репозитории" : "Показать репозитории";
    }
    private String getSessionStatusButton (boolean sessionIsActive) {
        return sessionIsActive ? "Остановить сессию" : "Запустить сессию";
    }
    private String getSessionStatus(boolean sessionIsActive) {
        return sessionIsActive ? "\uD83D\uDFE2 Сессия запущена" : "\uD83D\uDD34 Сессия остановлена";
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

        List<InlineButtonInfo> expirationTimer = new ArrayList<>();
        expirationTimer.add(new InlineButtonInfo(getExpirationTimerText(configuration), "EXPIRATION_NOTIFICATION"));

        List<InlineButtonInfo> accept = new ArrayList<>();
        accept.add(new InlineButtonInfo("Применить настройки", "SETTINGS_ACCEPT"));

        settings.add(country);
        settings.add(settingsButtons);
        settings.add(expirationTimer);
        settings.add(accept);
        return settings;
    }
    private String getExpirationTimerText (Configuration configuration) {
        return configuration.getExpirationTime() == 0? "Время удаление не задано" : "Время удаления составлят " + configuration.getExpirationTime() + " сек.";
    }
    private String onOrOff (boolean confInfo) {
        return confInfo ? "✅" : "❌";
    }
    private String flag (Locale locale) {
        return locale.getLanguage().equals("ru") ? "\uD83C\uDDF7\uD83C\uDDFA" : "\uD83C\uDDEC\uD83C\uDDE7";
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
}

