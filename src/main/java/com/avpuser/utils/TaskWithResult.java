package com.avpuser.utils;

@FunctionalInterface
public interface TaskWithResult<T> {
    T execute() throws Exception;
}
