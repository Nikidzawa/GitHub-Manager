package ru.nikidzawa.github_manager;

import org.kohsuke.github.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GitHubManager {
    private final GitHub gitHub;
    private GUI gui;
    private ResourceBundle messages;
    private GHMyself myself;
    private Set<Long> allPullsIds = new HashSet<>();
    private Set<String> allCommitsSHAs = new HashSet<>();
    private HashMap <Long, Integer> repoStars = new HashMap<>();
    public GitHubManager() {
        try {
            gitHub = new GitHubBuilder()
                    .withAppInstallationToken(System.getenv("GITHUB_TOKEN"))
                    .build();
            myself = gitHub.getMyself();
            gui =  new GUI();
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void init() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                messages = ResourceBundle.getBundle("messages", Configuration.selectedLocale);
                HashSet<GHPullRequest> newPullRequests = new HashSet<>();
                HashSet<GHCommit> newCommits = new HashSet<>();
                boolean checkFirstPull = !allPullsIds.isEmpty();
                boolean checkFirstCommit = !allCommitsSHAs.isEmpty();
                try {
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
                        newPullRequests.forEach(pr ->
                                gui.sendMessage(messages.getString("newPullRequestMessage") +
                                        pr.getRepository().getName(), pr.getTitle())
                        );
                    }
                    if (checkFirstCommit && Configuration.showCommits) {
                        newCommits.forEach(cm ->
                        {
                            try {
                                gui.sendMessage(messages.getString("newCommitMessage") +
                                                cm.getOwner().getName(), cm.getCommitShortInfo().getMessage());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    if (Configuration.showStars) {checkStars();}
                    gui.setMenu(myself.getLogin(), repos);
                } catch (Exception ex) {
                    throw new RuntimeException();
                }
            }
        }, 1000, 1000 );
    }
    private List<GHPullRequest> checkPulls(GHRepository repository, HashSet<GHPullRequest> newPullRequests) {
        try {
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

        } catch (IOException ex) {
            throw new RuntimeException();
        }
    }
    private List<GHCommit> checkCommits(GHRepository repository, HashSet<GHCommit> newCommits) {
        try {
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

        } catch (IOException ex) {
            throw new RuntimeException();
        }
    }
    private void checkStars() {
        try {
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
                                gui.sendMessage(messages.getString("like"),
                                        messages.getString("like_message") + repository.getName());
                            } else if (currentStars < previousStars) {
                                repoStars.put(id, currentStars);
                                gui.sendMessage(messages.getString("dislike"),
                                        messages.getString("dislike_message") + repository.getName());
                            }
                        }
                    });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }
}
