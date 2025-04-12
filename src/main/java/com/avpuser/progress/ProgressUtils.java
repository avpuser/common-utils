package com.avpuser.progress;

public class ProgressUtils {

    public static String getTelegramEmojiProgressBar(int percent) {
        int totalBlocks = 10;
        int filledBlocks = (percent * totalBlocks) / 100;
        int emptyBlocks = totalBlocks - filledBlocks;

        String bar = "🟩".repeat(filledBlocks) + "⬜".repeat(emptyBlocks); // Green and white blocks
        return bar + " " + percent + "%";
    }

}
