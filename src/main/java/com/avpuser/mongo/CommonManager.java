package com.avpuser.mongo;

import com.mongodb.client.result.DeleteResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class CommonManager<T extends DbEntity> {

    protected static final Logger logger = LogManager.getLogger(CommonManager.class);

    private final Class<T> type;

    private final CommonDao<T> dao;

    private CommonManager(CommonDao<T> dao, Class<T> type) {
        this.type = type;
        this.dao = dao;
    }

    public CommonManager(Map<Class<?>, CommonDao<? extends DbEntity>> allDaos, Class<T> type) {
        this(DaoRegistryUtil.getDao(allDaos, type), type);
    }

    public CommonManager(CommonDao<T> dao) {
        this.dao = dao;
        this.type = dao.getType();
    }

    public String insert(T entity) {
        return dao.insert(entity);
    }

    public void update(T entity) {
        dao.update(entity);
    }

    public Optional<T> findById(String id) {
        return dao.findById(id);
    }

    public void forEachEntity(Consumer<T> consumer) {
        dao.forEachEntity(consumer);
    }

    public T findByIdOrThrow(String id) {
        return dao.findByIdOrThrow(id);
    }

    public List<T> findByIds(List<String> ids) {
        return dao.findByIds(ids);
    }

    public List<T> findAll() {
        return dao.findAll();
    }

    public List<T> findBySpecification(LimitSpecification specification) {
        return dao.findBySpecification(specification);
    }

    public DeleteResult deleteBySpecification(LimitSpecification specification) {
        return dao.deleteBySpecification(specification);
    }

    public boolean existsById(String id) {
        return dao.existsById(id);
    }

    public boolean deleteById(String id) {
        return dao.deleteById(id);
    }

    public void deleteAll() {
        dao.deleteAll();
    }

    public long count() {
        return dao.count();
    }

    public final Optional<T> findSingleBySpecification(LimitSpecification specification) {
        return dao.findSingleBySpecification(specification);
    }

}
