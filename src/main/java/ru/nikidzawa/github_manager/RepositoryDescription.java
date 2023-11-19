package ru.nikidzawa.github_manager;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.util.List;

public class RepositoryDescription {
    private String name;
    private GHRepository ghRepository;
    private List<GHPullRequest> pullRequests;
    private List<GHCommit> commits;

    public RepositoryDescription (String name, GHRepository ghRepository) {
        this.name = name;
        this.ghRepository = ghRepository;
    }

    public void setPullRequests(List<GHPullRequest> pullRequests) {this.pullRequests = pullRequests;}

    public void setCommits(List<GHCommit> commits) {this.commits = commits;}

    public String getName() {
        return name;
    }

    public GHRepository getGhRepository() {
        return ghRepository;
    }

    public List<GHPullRequest> getPullRequests() {
        return pullRequests;
    }

    public List<GHCommit> getCommits() {return commits;}
}
