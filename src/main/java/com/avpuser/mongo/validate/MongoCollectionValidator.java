package com.avpuser.mongo.validate;

import com.avpuser.mongo.DbEntity;
import com.avpuser.utils.ReflectionsUtils;
import org.mongojack.MongoCollection;

import java.util.Set;

public class MongoCollectionValidator {

    public static void validate() {
        Set<Class<? extends DbEntity>> classes = ReflectionsUtils.getSubTypesOf(DbEntity.class);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(SkipMongoCollectionValidation.class)) {
                continue; // Skip validation for explicitly marked classes
            }

            MongoCollection annotation = clazz.getAnnotation(MongoCollection.class);
            if (annotation == null) {
                throw new IllegalStateException("Missing @MongoCollection on " + clazz.getName());
            }

            String expected = toSnakeCase(clazz.getSimpleName());
            String actual = annotation.name();

            if (!expected.equals(actual)) {
                throw new IllegalStateException("Incorrect @MongoCollection name for class " + clazz.getName()
                        + ". Expected: " + expected + ", but found: " + actual);
            }
        }
    }

    private static String toSnakeCase(String input) {
        return input
                .replaceAll("(?<=[a-z0-9])([A-Z])", "_$1") // добавляет _ перед заглавной буквой, если перед ней строчная или цифра
                .replaceAll("(?<=[A-Z])([A-Z][a-z])", "_$1") // разделяет ABBc → AB_Bc
                .toLowerCase();
    }
}