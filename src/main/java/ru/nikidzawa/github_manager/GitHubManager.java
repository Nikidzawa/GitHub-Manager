package ru.nikidzawa.github_manager;

import org.kohsuke.github.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GitHubManager {
    private final GitHub gitHub;
    private final GUI gui = new GUI();
    @SuppressWarnings("deprecation")
    Locale selectedLocale = new Locale("ru");
    ResourceBundle messages = ResourceBundle.getBundle("messages", selectedLocale);
    GHMyself myself;
    Set<Long> allPrIds = new HashSet<>();
    Set<String> allCmSHAs = new HashSet<>();
    Set<String> allBrSHAs = new HashSet<>();
    HashMap <Long, Integer> repoStars = new HashMap<>();
    public GitHubManager() {
        try {
            gitHub = new GitHubBuilder()
                    .withAppInstallationToken(System.getenv("GITHUB_TOKEN"))
                    .build();
            myself = gitHub.getMyself();
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void init() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                checkCommits();
                checkPulls();
                checkStars();
            }
        }, 1000, 1000 );
    }
    private void checkPulls() {
        try {
            HashSet<GHPullRequest> newPrs = new HashSet<>();
            boolean checkFirst = !allPrIds.isEmpty();

            myself.getAllRepositories()
                    .values()
                    .stream()
                    .map(repository -> {
                        try {
                            List<GHPullRequest> prs = repository.queryPullRequests()
                                    .list()
                                    .toList();
                            Set<Long> prIds = prs.stream()
                                    .map(GHPullRequest::getId)
                                    .collect(Collectors.toSet());
                            prIds.removeAll(allPrIds);
                            allPrIds.addAll(prIds);
                            prs.forEach(pr -> {
                                if (prIds.contains(pr.getId())) {
                                    newPrs.add(pr);
                                }
                            });
                            return new RepositoryDescription (
                                    repository.getFullName(),
                                    repository,
                                    prs
                            );
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            if (checkFirst) {
                newPrs.forEach(pr ->
                        gui.sendMessage(messages.getString("newPullRequestMessage") +
                                pr.getRepository().getName(), pr.getTitle())
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void checkCommits() {
        try {
            HashSet<GHCommit> newCms = new HashSet<>();
            boolean checkFirst = !allCmSHAs.isEmpty();
            myself.getAllRepositories()
                    .values()
                    .stream()
                    .map(repository -> {
                        try {
                            List<GHCommit> cms = repository.queryCommits()
                                    .list()
                                    .toList();
                            Set<String> cmSHAs = cms.stream()
                                    .map(GHCommit::getSHA1)
                                    .collect(Collectors.toSet());
                            cmSHAs.removeAll(allCmSHAs);
                            allCmSHAs.addAll(cmSHAs);
                            cms.forEach(cm -> {
                                if (cmSHAs.contains(cm.getSHA1())) {
                                    newCms.add(cm);
                                }
                            });
                        }catch (IOException ex) {
                            throw new RuntimeException();
                        }
                        return null;
                    })
                    .collect(Collectors.toList());

            if (checkFirst) {
                newCms.forEach(cm ->
                {
                    try {
                        gui.sendMessage(messages.getString("newCommitMessage") + cm.getOwner().getName(),
                                cm.getCommitShortInfo().getMessage());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

        }catch (IOException ex) {
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
                                    gui.sendMessage("Лайк", "Новый лайк в репозитории"
                                            + repository.getName());
                                } else if (currentStars < previousStars) {
                                    repoStars.put(id, currentStars);
                                    gui.sendMessage("Дизлайк", "Потерян лайк в репозитории"
                                            + repository.getName());
                                }
                            }
                        });
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
    }
}
