package com.avpuser.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class ReflectionsUtils {

    private static final Logger logger = LogManager.getLogger(ReflectionsUtils.class);

    /**
     * Scans the full classpath and returns all subtypes of the given class or interface.
     *
     * @param type The base class or interface to search for subtypes.
     * @param <T>  The type of the base class/interface.
     * @return Set of discovered subtypes.
     */
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

    /**
     * Scans the full classpath and returns all static, no-arg methods annotated with the given annotation.
     * Intended for general use such as annotation-based configuration or index registration.
     *
     * @param annotationClass The annotation to look for.
     * @return Set of matched static, no-arg methods.
     */
    public static Set<Method> getStaticNoArgAnnotatedMethods(Class<? extends Annotation> annotationClass) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forJavaClassPath())
                .setScanners(Scanners.MethodsAnnotated));

        Set<Method> result = new HashSet<>();
        Set<Method> methods = reflections.getMethodsAnnotatedWith(annotationClass);

        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
                result.add(method);
            }
        }

        logger.info("Found " + result.size() + " static no-arg methods annotated with @" + annotationClass.getSimpleName());
        return result;
    }

}
