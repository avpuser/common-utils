package com.avpuser.progress;

public interface ProgressListener {

    void onProgress(int percent);  // Called to update progress

    void onComplete();            // Called when processing is complete

    void onError(String error);   // Called in case of an error
}
