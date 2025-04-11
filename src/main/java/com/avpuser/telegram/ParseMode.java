package com.avpuser.telegram;

public enum ParseMode {
    MARKDOWN("Markdown"),
    MARKDOWNV2("MarkdownV2"),
    HTML("HTML"),
    NONE("");

    private final String mode;

    ParseMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return this.mode;
    }
}