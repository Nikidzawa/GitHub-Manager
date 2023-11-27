package ru.nikidzawa.github_manager;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.util.List;

@Getter
@Setter
public class RepositoryDescription {
    private String name;
    private GHRepository ghRepository;
    private List<GHPullRequest> pullRequests;
    private List<GHCommit> commits;
    private int starsCount;

    public RepositoryDescription (String name, GHRepository ghRepository) {
        this.name = name;
        this.ghRepository = ghRepository;
    }
}
