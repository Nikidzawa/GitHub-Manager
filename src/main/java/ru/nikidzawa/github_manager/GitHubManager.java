package ru.nikidzawa.github_manager;

import lombok.Getter;
import lombok.SneakyThrows;
import org.antlr.v4.runtime.misc.Pair;
import org.kohsuke.github.*;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.nikidzawa.github_manager.desktop.gui.GUI;
import ru.nikidzawa.github_manager.telegram.service.Crypto;
import ru.nikidzawa.github_manager.telegram.service.TelegramBot;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GitHubManager {
    private ResourceBundle messages;
    private GHMyself myself;
    private Set<Long> allPullsIds = new HashSet<>();
    private Set<String> allCommitsSHAs = new HashSet<>();
    private HashMap <Long, Integer> repoStars = new HashMap<>();
    private Timer timer;
    @Getter
    private GitHub gitHub;
    private Configuration configuration;
    @Getter
    private List<RepositoryDescription> repos;
    TimerTask timerTask;
    TelegramBot telegramBot;
    Long userId;
    private boolean isActive;

    @SneakyThrows
    public GitHubManager() {
        GitHub gitHub = new GitHubBuilder()
                .withAppInstallationToken("Ваш токен")
                .build();
        myself = gitHub.getMyself();
        configuration = new Configuration();
        GUI gui = new GUI();
        startDesktopSession(gui);
    }

    @SneakyThrows
    public GitHubManager(Long userId, byte[] token, Configuration configuration, TelegramBot telegramBot) {
        this.configuration = configuration;
        this.telegramBot = telegramBot;
        this.userId = userId;
        try {
            gitHub = new GitHubBuilder()
                        .withAppInstallationToken(Crypto.decryptData(token))
                        .build();
            myself = gitHub.getMyself();
        } catch (Exception e ) {
            telegramBot.sendMessage(userId, "Неверный токен ❌");
            throw new RuntimeException(e);
        }
        isActive = true;
        telegramBot.wait.remove(userId);
        startTelegramSession();
    }
    private void startDesktopSession(GUI gui) {
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
                                gui.sendMessage(messages.getString("newPullRequestMessage") +
                                        pr.getRepository().getName(), pr.getTitle());
                        });
                    }
                    if (checkFirstCommit && configuration.isShowCommits()) {
                        newCommits.forEach(cm -> {
                            try {
                                gui.sendMessage(messages.getString("newCommitMessage") +
                                        cm.getOwner().getName(), cm.getCommitShortInfo().getMessage());
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                    }
                    if (configuration.isShowStars()) {
                        Pair<List<GHRepository>, List<GHRepository>> stars = checkStars();
                        stars.a.forEach(repository -> gui.sendMessage(messages.getString("like"),
                                messages.getString("like_message") + repository.getName()));
                        stars.b.forEach(repository -> gui.sendMessage(messages.getString("dislike"),
                                messages.getString("dislike_message") + repository.getName()));
                    }
                    gui.setMenu(myself.getLogin(), repos, configuration);
                } catch (Exception ex) {
                    gui.sendMessage("Ошибка", "Произошла ошибка, попробуйте ещё раз");
                    throw new RuntimeException(ex);
                }
            }
        }, 1000, 1000 );
    }

    private void startTelegramSession() {
        timer = new Timer();
        timer.schedule(timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    messages = ResourceBundle.getBundle("messages", configuration.getSelectedLocale());
                    HashSet<GHPullRequest> newPullRequests = new HashSet<>();
                    HashSet<GHCommit> newCommits = new HashSet<>();
                    boolean checkFirstPull = !allPullsIds.isEmpty();
                    boolean checkFirstCommit = !allCommitsSHAs.isEmpty();

                     repos = myself.getAllRepositories()
                            .values()
                            .stream()
                            .map(repository -> {
                                RepositoryDescription repoDisc = new RepositoryDescription(
                                        repository.getName(), repository);
                                repoDisc.setPullRequests(checkPulls(repository, newPullRequests));
                                repoDisc.setCommits(checkCommits(repository, newCommits));
                                repoDisc.setStarsCount(repository.getStargazersCount());
                                return repoDisc;
                            }).toList();

                    if (checkFirstPull && configuration.isShowPullRequests()) {
                        newPullRequests.forEach(pr -> {
                            Pair<String, String> repo = new Pair<>("Репозиторий \uD83D\uDE80", pr.getRepository().getHtmlUrl().toString());
                            Pair<String, String> pull = new Pair<>("Открыть реквест \uD83D\uDE80", pr.getHtmlUrl().toString());
                            List<Pair<String, String>> list = new ArrayList<>();
                            list.add(repo);
                            list.add(pull);
                                InlineKeyboardMarkup markup = telegramBot.urlIneKeyBoardMarkupBuilder(list);
                                telegramBot.sendMessageInlineMarkupAndDelete(userId,"⚓ " + messages.getString("newPullRequestMessage") +
                                        pr.getRepository().getName() + "\nОписание: " + pr.getTitle(), markup, configuration.getExpirationTime());
                        });
                    }
                    if (checkFirstCommit && configuration.isShowCommits()) {
                        newCommits.forEach(cm -> {
                            try {
                                Pair<String, String> repo = new Pair<>("Репозиторий \uD83D\uDE80", cm.getOwner().getHtmlUrl().toString());
                                Pair<String, String> commit = new Pair<>("Посмотреть коммит \uD83D\uDE80", cm.getHtmlUrl().toString());
                                List<Pair<String, String>> list = new ArrayList<>();
                                list.add(repo);
                                list.add(commit);
                                    InlineKeyboardMarkup markup = telegramBot.urlIneKeyBoardMarkupBuilder(list);
                                    telegramBot.sendMessageInlineMarkupAndDelete(userId,"⚡ " + messages.getString("newCommitMessage") +
                                            cm.getOwner().getName() + "\nОписание: " + cm.getCommitShortInfo().getMessage(), markup, configuration.getExpirationTime());
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                    }
                    if (configuration.isShowStars()) {
                        Pair<List<GHRepository>, List<GHRepository>> stars = checkStars();
                        stars.a.forEach(repository -> {
                            InlineKeyboardMarkup markup = telegramBot.urlIneKeyBoardMarkupBuilder(
                                    "Перейти в репозиторий \uD83D\uDE80", repository.getHtmlUrl().toString());
                            telegramBot.sendMessageInlineMarkupAndDelete(userId, "⭐ " + messages.getString("like")
                                    + "\n" + messages.getString("like_message") + repository.getName(), markup, configuration.getExpirationTime());
                        });
                        stars.b.forEach(repository -> {
                            InlineKeyboardMarkup markup = telegramBot.urlIneKeyBoardMarkupBuilder(
                                    "Перейти в репозиторий \uD83D\uDE80", repository.getHtmlUrl().toString());
                            telegramBot.sendMessageInlineMarkupAndDelete(userId, "\uD83D\uDE31 " + messages.getString("dislike") +
                                    "\n" + messages.getString("dislike_message") + repository.getName(), markup, configuration.getExpirationTime());
                        });
                    }
                }catch (RuntimeException | IOException ex) {
                    telegramBot.sendMessage(userId, "Произошла ошибка, поробуйте снова через  или свяжитесь с @Nikidzawa");
                    throw new RuntimeException(ex);
                }
            }
        },0, 10000 );
    }

    public void stopSession() {
        isActive = false;
        timer.cancel();
        timer.purge();
    }
    public void startSession() {
        isActive = true;
        startTelegramSession();
    }
    public boolean sessionIsActive() {
        return isActive;
    }

    @SneakyThrows
    private List<GHPullRequest> checkPulls (GHRepository repository, HashSet<GHPullRequest> newPullRequests) {
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
    private List<GHCommit> checkCommits (GHRepository repository, HashSet<GHCommit> newCommits) {
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
    private Pair<List<GHRepository>, List<GHRepository>> checkStars () {
        List<GHRepository> likes = new ArrayList<>();
        List<GHRepository> disLikes = new ArrayList<>();
            myself.getAllRepositories()
                    .values()
                    .forEach(repository -> {
                        long id = repository.getId();
                        int currentStars = repository.getStargazersCount();
                        if (!repoStars.containsKey(id)) {
                            repoStars.put(id, currentStars);
                        }
                        else {
                            int previousStars = repoStars.get(id);
                            if (currentStars > previousStars) {
                                likes.add(repository);
                                repoStars.put(id, currentStars);
                            }
                            else if (currentStars < previousStars) {
                                repoStars.put(id, currentStars);
                                disLikes.add(repository);
                            }
                        }
                    });
            return new Pair<>(likes, disLikes);
    }
}
