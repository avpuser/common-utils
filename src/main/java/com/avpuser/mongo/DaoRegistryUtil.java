package com.avpuser.mongo;

import java.util.Map;

public class DaoRegistryUtil {
    @SuppressWarnings("unchecked")
    public static <T extends DbEntity> CommonDao<T> getDao(Map<Class<?>, CommonDao<? extends DbEntity>> allDaos, Class<T> clazz) {
        CommonDao<? extends DbEntity> dao = allDaos.get(clazz);
        if (dao == null) {
            throw new IllegalStateException("No DAO found for class: " + clazz.getName());
        }
        return (CommonDao<T>) dao;
    }
}
