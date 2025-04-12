package com.avpuser.progress;

public class SubProgressListener implements ProgressListener {

    private final ProgressListener parent;

    private final int startPercent;

    private final int endPercent;

    private SubProgressListener(ProgressListener parent, int startPercent, int endPercent) {
        if (startPercent < 0 || endPercent > 100 || startPercent > endPercent) {
            throw new IllegalArgumentException("Invalid percentage range");
        }
        this.parent = parent;
        this.startPercent = startPercent;
        this.endPercent = endPercent;
    }

    public static ProgressListener create(ProgressListener parent, int startPercent, int endPercent) {
        if (parent == null || parent instanceof EmptyProgressListener) {
            return new EmptyProgressListener();
        }
        return new SubProgressListener(parent, startPercent, endPercent);
    }

    @Override
    public void onProgress(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100");
        }
        // Scale percentage from range [0, 100] to [startPercent, endPercent]
        int scaledProgress = startPercent + (int) ((endPercent - startPercent) * (percent / 100.0));
        parent.onProgress(scaledProgress); // We inform the parent
    }

    @Override
    public void onComplete(String message) {
        parent.onProgress(endPercent); // Progress to 100% at the end

        if (endPercent == 100) {
            parent.onComplete(message);          // Completing the parent
        }
    }

    @Override
    public void onError(String error) {
        parent.onError(error); // We pass the error to the parent
    }
}
