package com.avpuser.progress;

@FunctionalInterface
public interface TaskWithResult<T> {
    T execute() throws Exception;
}
