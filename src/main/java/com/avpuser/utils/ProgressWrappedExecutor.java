package com.avpuser.utils;

import com.avpuser.progress.ProgressListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressWrappedExecutor {

    public static <T> T runWithProgress(TaskWithResult<T> task, ProgressListener progressListener) {
        progressListener.onProgress(5);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicInteger progress = new AtomicInteger(5);

            Future<?> progressFuture = executor.submit(() -> {
                try {
                    while (!completed.get() && progress.get() < 99) {
                        progressListener.onProgress(progress.getAndAdd(1));
                        Thread.sleep(1_000);
                    }
                } catch (InterruptedException ignored) {
                }
            });

            Future<T> resultFuture = executor.submit(() -> task.execute());

            try {
                T result = resultFuture.get();
                completed.set(true);
                progressFuture.cancel(true);
                progressListener.onProgress(99);
                return result;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error while executing task with progress", e);
            } finally {
                executor.shutdownNow();
            }
        }
    }
}