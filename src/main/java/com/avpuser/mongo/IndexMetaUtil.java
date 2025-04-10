package com.avpuser.mongo;

import com.avpuser.utils.ReflectionsUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IndexMetaUtil {

    private static final Logger logger = LogManager.getLogger(IndexMetaUtil.class);

    public static Set<Class<? extends LimitSpecification>> findClassesExtendingLimitSpecification() {
        return ReflectionsUtils.getSubTypesOf(LimitSpecification.class);
    }

    public static List<IndexMeta> generateAllIndexMeta() {
        List<IndexMeta> indexMetas = new ArrayList<>();
        Set<Class<? extends LimitSpecification>> classes = findClassesExtendingLimitSpecification();

        for (Class<?> clazz : classes) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(IndexMetaGenerator.class) &&
                        method.getReturnType() == IndexMeta.class &&
                        method.getParameterCount() == 0 &&
                        Modifier.isStatic(method.getModifiers())) {

                    try {
                        IndexMeta indexMeta = (IndexMeta) method.invoke(null);
                        indexMetas.add(indexMeta);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return indexMetas;
    }
}
