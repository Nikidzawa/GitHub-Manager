package ru.nikidzawa.github_manager.telegram.service;

import lombok.Getter;

@Getter
public class InlineButtonInfo {
    private String text;
    private String callbackData;
    public InlineButtonInfo(String text, String callbackData) {
        this.text = text;
        this.callbackData = callbackData;
    }
}
