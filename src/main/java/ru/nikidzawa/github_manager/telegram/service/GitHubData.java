package ru.nikidzawa.github_manager.telegram.service;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import ru.nikidzawa.github_manager.Configuration;
import ru.nikidzawa.github_manager.GitHubManager;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitHubData {
    Configuration configuration;
    GitHub gitHub;
    GitHubManager gitHubManager;
    List<GHRepository> bannedRepos;
    public GitHubData () {
        this.configuration = new Configuration();
    }
    public GitHubData (Configuration configuration, GitHub gitHub, GitHubManager gitHubManager) {
        this.configuration = configuration;
        this.gitHub = gitHub;
        this.gitHubManager = gitHubManager;
    }
}
