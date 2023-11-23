package ru.nikidzawa.github_manager.desktop;

import lombok.SneakyThrows;
import org.kohsuke.github.*;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.nikidzawa.github_manager.telegram.service.TelegramBot;

import java.util.*;
import java.util.stream.Collectors;

public class GitHubManager {
    private GitHub gitHub;
    private GUI gui;
    private ResourceBundle messages;
    private GHMyself myself;
    private Set<Long> allPullsIds = new HashSet<>();
    private Set<String> allCommitsSHAs = new HashSet<>();
    private HashMap <Long, Integer> repoStars = new HashMap<>();
    private TelegramBot telegramBot;
    private Long userId;
    private Timer timer;
    private Configuration configuration;

    @SneakyThrows
    public GitHubManager() {
        configuration = new Configuration();
        gitHub = new GitHubBuilder()
                .withAppInstallationToken("Ваш токен")
                .build();
        myself = gitHub.getMyself();
        gui = new GUI();
        init();
    }

    public GitHubManager(Long id, String token, Configuration configuration, TelegramBot telegramBot) {
        try {
            this.configuration = configuration;
            this.telegramBot = telegramBot;
            userId = id;
            gitHub = new GitHubBuilder()
                    .withAppInstallationToken(token)
                    .build();
            myself = gitHub.getMyself();
        }catch (Exception ex) {
            telegramBot.sendMessage(userId, "Неверный токен или сбой в работе GitHub Api");
            throw new RuntimeException();
        }
        telegramBot.wait.remove(userId);
        telegramBot.sendMessage(userId,
                "Ответ получен, приложение начало свою работу!" +
                        "\n /settings для индивидуальной настройки." +
                        "\n Отзыв о пожеланиях и ошибках можете оставить у меня в личных сообщениях @Nikidzawa." +
                        "\n Десктопную версию можете найти в моём GitHub https://github.com/Nikidzawa/GitHub-Manager." +
                        "\n Приятного пользоавния!");
        init();
    }

    // TODO: 24.11.2023 Помимо ссылок на репозитории, нужно добавить ссылки на коммиты/пуллы
    // TODO: 24.11.2023 Добавить смайлики
    // TODO: 24.11.2023 Сократить код, особенно ifы
    private void init() {
        timer = new Timer();
         timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    messages = ResourceBundle.getBundle("messages", configuration.getSelectedLocale());
                    HashSet<GHPullRequest> newPullRequests = new HashSet<>();
                    HashSet<GHCommit> newCommits = new HashSet<>();
                    boolean checkFirstPull = !allPullsIds.isEmpty();
                    boolean checkFirstCommit = !allCommitsSHAs.isEmpty();

                    List<RepositoryDescription> repos = myself.getAllRepositories()
                            .values()
                            .stream()
                            .map(repository -> {
                                RepositoryDescription repoDisc = new RepositoryDescription(
                                        repository.getName(), repository);
                                repoDisc.setPullRequests(checkPulls(repository, newPullRequests));
                                repoDisc.setCommits(checkCommits(repository, newCommits));
                                return repoDisc;
                            }).toList();
                    if (checkFirstPull && configuration.isShowPullRequests()) {
                        newPullRequests.forEach(pr -> {
                            if (telegramBot != null) {
                                InlineKeyboardMarkup markup = telegramBot.urlIneKeyBoardMarkupBuilder(
                                        "Перейти в репозиторий", pr.getRepository().getHtmlUrl().toString());
                                telegramBot.sendMessageInlineMarkup(userId, messages.getString("newPullRequestMessage") +
                                        pr.getRepository().getName() + "\n" + pr.getTitle(), markup);
                            } else {
                                gui.sendMessage(messages.getString("newPullRequestMessage") +
                                        pr.getRepository().getName(), pr.getTitle());
                            }
                        });
                    }
                    if (checkFirstCommit && configuration.isShowCommits()) {
                        newCommits.forEach(cm -> {
                            try {
                                if (telegramBot != null) {
                                    InlineKeyboardMarkup markup = telegramBot.urlIneKeyBoardMarkupBuilder(
                                            "Перейти в репозиторий", cm.getOwner().getHtmlUrl().toString());
                                    telegramBot.sendMessageInlineMarkup(userId, messages.getString("newCommitMessage") +
                                            cm.getOwner().getName() + "\n" + cm.getCommitShortInfo().getMessage(), markup);
                                } else {
                                    gui.sendMessage(messages.getString("newCommitMessage") +
                                            cm.getOwner().getName(), cm.getCommitShortInfo().getMessage());
                                }
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                    }
                    if (configuration.isShowStars()) {
                        checkStars();
                    }
                    if (telegramBot == null) {
                        gui.setMenu(myself.getLogin(), repos, configuration);
                    }
                }catch (Exception ex) {
                    if (telegramBot != null) telegramBot.sendMessage(userId, "Произошла ошибка, поробуйте снова или свяжитесь с @Nikidzawa");
                    throw new RuntimeException(ex);
                }

            }
        }, 1000, 1000 );
    }
    public void stopTimer() {
        timer.cancel();
        timer.purge();
    }

    @SneakyThrows
    private List<GHPullRequest> checkPulls(GHRepository repository, HashSet<GHPullRequest> newPullRequests) {
        List<GHPullRequest> pullRequests = repository.queryPullRequests()
                .list()
                .toList();
        Set<Long> requestsIds = pullRequests.stream()
                .map(GHPullRequest::getId)
                .collect(Collectors.toSet());
        requestsIds.removeAll(allPullsIds);
        allPullsIds.addAll(requestsIds);
        pullRequests.forEach(pr -> {
            if (requestsIds.contains(pr.getId())) {
                newPullRequests.add(pr);
            }
        });
        return pullRequests;
    }
    @SneakyThrows
    private List<GHCommit> checkCommits(GHRepository repository, HashSet<GHCommit> newCommits) {
            List<GHCommit> commits = repository.queryCommits()
                    .list()
                    .toList();
            Set<String> commitsSHAs = commits.stream()
                    .map(GHCommit::getSHA1)
                    .collect(Collectors.toSet());
            commitsSHAs.removeAll(allCommitsSHAs);
            allCommitsSHAs.addAll(commitsSHAs);
            commits.forEach(cm -> {
                if (commitsSHAs.contains(cm.getSHA1())) {
                    newCommits.add(cm);
                }
            });
        return commits;
    }
    @SneakyThrows
    private void checkStars() {
            myself.getAllRepositories()
                    .values()
                    .forEach(repository -> {
                        long id = repository.getId();
                        int currentStars = repository.getStargazersCount();
                        if (!repoStars.containsKey(id)) {
                            repoStars.put(id, currentStars);
                        } else {
                            int previousStars = repoStars.get(id);
                            InlineKeyboardMarkup markup = telegramBot.urlIneKeyBoardMarkupBuilder(
                                    "Перейти в репозиторий", repository.getHtmlUrl().toString());
                            if (currentStars > previousStars) {
                                repoStars.put(id, currentStars);
                                if (telegramBot != null) {
                                    telegramBot.sendMessageInlineMarkup(userId, messages.getString("like")
                                            + "\n"
                                            + messages.getString("like_message") + repository.getName(), markup);
                                }
                                else gui.sendMessage(messages.getString("like"),
                                            messages.getString("like_message") + repository.getName());

                            }
                            else if (currentStars < previousStars) {
                                repoStars.put(id, currentStars);
                                if (telegramBot != null) {
                                    telegramBot.sendMessageInlineMarkup(userId, messages.getString("dislike")
                                            + "\n"
                                            + messages.getString("dislike_message") + repository.getName(), markup);
                                }
                                else {
                                    gui.sendMessage(messages.getString("dislike"),
                                            messages.getString("dislike_message") + repository.getName());
                                }
                            }
                        }
                    });

    }
}
