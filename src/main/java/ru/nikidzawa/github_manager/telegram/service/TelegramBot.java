package ru.nikidzawa.github_manager.telegram.service;

import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
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
        listOfCommands.add(new BotCommand("/start", "Начать"));
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
    private HashMap<Long, GitHubManager> gitHub = new HashMap<>();
    @SneakyThrows
    @Override
    @Transactional
    public void onUpdateReceived(Update update) {
        Long userId = update.hasCallbackQuery() ?
                update.getCallbackQuery().getMessage().getChatId() : update.getMessage().getChatId();
        // TODO: 23.11.2023  
        String message = null;
        if (!update.hasCallbackQuery()) {
             message = update.getMessage().getText();
        }

        GitHubManager gitHubManager = gitHub.getOrDefault(userId, null);

        UserEntity user = repository.findById(userId).orElseGet(() -> {
            UserEntity entity = new UserEntity();
            entity.setId(userId);
            entity.setLanguage(new Locale("en"));
            repository.save(entity);
            return entity;
        });

        ResourceBundle messages = ResourceBundle.getBundle("messages", user.getLanguage());

        if (update.hasMessage() && update.getMessage().hasText() && !wait.contains(userId)) {
            switch (message) {
                case "/start" :
                    sendMessageInlineMarkup(userId, "Я ваш персональный GitHub помощник, " +
                            "который будет уведомлять вас об изменениях в репозиториях\n" +
                            "Для продолженя, введите ваш GitHub токен", null);
                    wait.add(userId);
                    break;
                case "/language":
                    List<InlineButtonInfo> buttonInfoList = new ArrayList<>();
                    buttonInfoList.add(new InlineButtonInfo("Russian", "RUSSIAN"));
                    buttonInfoList.add(new InlineButtonInfo("English", "ENGLISH"));
                    InlineKeyboardMarkup inlineKeyboardMarkup = createMarkup(buttonInfoList);

                    sendMessageInlineMarkup(userId, "Chose your language", inlineKeyboardMarkup);
                    break;
            }
        }
        else if (update.hasCallbackQuery() && !wait.contains(userId)) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            switch (callBackData) {
                case "RUSSIAN":
                    EditMessageText RUSSIAN = new EditMessageText();
                    RUSSIAN.setChatId(userId);
                    RUSSIAN.setText("Вы выбрали русский язык");
                    RUSSIAN.setMessageId((int) messageId);
                    user.setLanguage(new Locale("ru"));
                    repository.save(user);
                    execute(RUSSIAN);
                    break;
                case "ENGLISH":
                    EditMessageText ENGLISH = new EditMessageText();
                    ENGLISH.setChatId(userId);
                    ENGLISH.setText("Вы выбрали английский язык");
                    ENGLISH.setMessageId((int) messageId);
                    user.setLanguage(new Locale("en"));
                    repository.save(user);
                    execute(ENGLISH);
                    break;
                case "BACK_TO_REPOSITORY":
                    break;
            }
        } else if (wait.contains(userId)) {
            user.setToken(message);
            repository.save(user);
            sendMessage(userId, "Токен был получен. Ожидание ответа от Github Api ");
            wait.remove(userId);

            gitHubManager = new GitHubManager(userId, message, this);

            gitHub.remove(userId);
            gitHub.put(userId, gitHubManager);

            sendMessage(user.getId(),
                    "Ответ получен, приложение начало свою работу!" +
                            "\n /settings для индивидуальной настройки." +
                            "\n Отзыв о пожеланиях и ошибках можете оставить у меня в личных сообщениях @Nikidzawa." +
                            "\n Десктопную версию можете найти в моём GitHub https://github.com/Nikidzawa/GitHub-Manager." +
                            "\n Приятного пользоавния!");
        }
    }
    public InlineKeyboardMarkup createMarkup (List<InlineButtonInfo> buttonInfoList) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (InlineButtonInfo buttonInfo : buttonInfoList) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonInfo.getText());
            button.setCallbackData(buttonInfo.getCallbackData());

            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
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
    public void sendMessage (Long id, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(id);
        sendMessage.setText(message);
        execute(sendMessage);
    }
}

