package ru.nikidzawa.github_manager;

import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
public class Configuration {
    private boolean showCommits = true;
    private boolean showPullRequests = true;
    private boolean showStars = true;
    private boolean showRepos = false;
    private Locale selectedLocale = new Locale("en");
}
