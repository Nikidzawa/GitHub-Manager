package ru.nikidzawa.github_manager.desktop;

import lombok.SneakyThrows;
import org.kohsuke.github.*;

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
    @SneakyThrows
    public GitHubManager() {
        gitHub = new GitHubBuilder()
                .withAppInstallationToken("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")
                .build();
        myself = gitHub.getMyself();
         gui = new GUI();
        init();
    }

    public GitHubManager(Long id, String token, TelegramBot telegramBot) {
        try {
            this.telegramBot = telegramBot;
            userId = id;
            gitHub = new GitHubBuilder()
                    .withAppInstallationToken(token)
                    .build();
            myself = gitHub.getMyself();
        }catch (Exception ex) {
            telegramBot.sendMessage(userId, "Неверный токен");
            telegramBot.wait.add(id);
            throw new RuntimeException();
        }
        init();
    }
    private void init() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    messages = ResourceBundle.getBundle("messages", Configuration.selectedLocale);
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
                    if (checkFirstPull && Configuration.showPullRequests) {
                        newPullRequests.forEach(pr -> {
                            if (telegramBot != null) {
                                telegramBot.sendMessage(userId, messages.getString("newPullRequestMessage") +
                                        pr.getRepository().getName() + "\n" + pr.getTitle());
                            } else {
                                gui.sendMessage(messages.getString("newPullRequestMessage") +
                                        pr.getRepository().getName(), pr.getTitle());
                            }
                        });
                    }
                    if (checkFirstCommit && Configuration.showCommits) {
                        newCommits.forEach(cm -> {
                            try {
                                if (telegramBot != null) {
                                    telegramBot.sendMessage(userId, messages.getString("newCommitMessage") +
                                            cm.getOwner().getName() + "\n" + cm.getCommitShortInfo().getMessage());
                                } else {
                                    gui.sendMessage(messages.getString("newCommitMessage") +
                                            cm.getOwner().getName(), cm.getCommitShortInfo().getMessage());
                                }
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                    }
                    if (Configuration.showStars) {
                        checkStars();
                    }
                    if (telegramBot == null) {
                        gui.setMenu(myself.getLogin(), repos);
                    }
                }catch (Exception ex) {
                    if (telegramBot != null) telegramBot.sendMessage(userId, messages.getString("exception"));
                    throw new RuntimeException(ex);
                }

            }
        }, 1000, 1000 );
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
                            if (currentStars > previousStars) {
                                repoStars.put(id, currentStars);
                                if (telegramBot != null) {
                                    telegramBot.sendMessage(userId, messages.getString("like") + "\n"
                                            + messages.getString("like_message") + repository.getName());
                                }
                                else gui.sendMessage(messages.getString("like"),
                                            messages.getString("like_message") + repository.getName());

                            } else if (currentStars < previousStars) {
                                repoStars.put(id, currentStars);
                                if (telegramBot != null)
                                    telegramBot.sendMessage(userId, messages.getString("dislike") + "\n"
                                            + messages.getString("dislike_message") + repository.getName());
                                else gui.sendMessage(messages.getString("dislike"),
                                        messages.getString("dislike_message") + repository.getName());
                            }
                        }
                    });

    }
}
