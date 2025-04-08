package com.avpuser.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.Set;

public class ReflectionsUtils {

    private static final Logger logger = LogManager.getLogger(ReflectionsUtils.class);

    public static <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forJavaClassPath()) // Scan the entire classpath
                .setScanners(new SubTypesScanner(false)));

        Set<Class<? extends T>> subTypesOf = reflections.getSubTypesOf(type);
        if (subTypesOf.isEmpty()) {
            logger.warn("No subtypes found for type: " + type.getName());
        } else {
            logger.info("Found " + subTypesOf.size() + " subtypes for type: " + type.getName());
        }
        return subTypesOf;
    }

}
