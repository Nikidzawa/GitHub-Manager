package ru.nikidzawa.github_manager;

import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.util.List;

public class RepositoryDescription {
    private String name;
    private GHRepository ghRepository;
    private List<GHPullRequest> prs;

    public RepositoryDescription(String name, GHRepository ghRepository, List<GHPullRequest> prs) {
        this.name = name;
        this.ghRepository = ghRepository;
        this.prs = prs;
    }

    public String getName() {
        return name;
    }

    public GHRepository getGhRepository() {
        return ghRepository;
    }

    public List<GHPullRequest> getPrs() {
        return prs;
    }
}
