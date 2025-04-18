package com.avpuser.mongo;

import com.mongodb.client.result.DeleteResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public void insert(T entity) {
        dao.insert(entity);
    }

    public void update(T entity) {
        dao.update(entity);
    }

    public Optional<T> findById(String id) {
        return dao.findById(id);
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


}

