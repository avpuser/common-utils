package com.avpuser.mongo;

import com.avpuser.mongo.typeconverter.MongoObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.mongojack.JacksonMongoCollection;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class CommonDao<T extends DbEntity> {

    private final static Logger logger = LogManager.getLogger(CommonDao.class);

    protected final JacksonMongoCollection<T> mongoCollection;

    private final Class<T> type;
    private final Clock clock;

    public CommonDao(MongoDatabase database, Class<T> type, Clock clock) {
        this.type = type;
        this.clock = clock;

        ObjectMapper objectMapper = MongoObjectMapperFactory.createObjectMapper();
        this.mongoCollection = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(database, type, UuidRepresentation.STANDARD);
    }

    public final Class<T> getType() {
        return type;
    }

    protected String getDbEntityName() {
        return type.getSimpleName();
    }

    public final String insert(T entity) {
        Instant now = clock.instant();

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        if (entity.getUpdatedAt() == null) {
            entity.setUpdatedAt(now);
        }
        mongoCollection.insert(entity);
        logger.info(getDbEntityName() + " saved successfully. " + entity);
        return entity.getId();
    }

    public final void update(T entity) {
        String id = entity.getId();
        if (!existsById(id)) {
            throw new RuntimeException("No" + getDbEntityName() + " with id: " + id);
        }

        Instant now = clock.instant();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);

        mongoCollection.replaceOne(Filters.eq("_id", id), entity);
        logger.info(getDbEntityName() + " updated successfully. " + id);
    }

    public final Optional<T> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        Optional<T> entity = Optional.ofNullable(mongoCollection.findOneById(id));
        if (entity.isEmpty()) {
            logger.info("Entity of type " + getDbEntityName() + " not found for id: " + id);
        }
        return entity;
    }

    public T findByIdOrThrow(String id) {
        return findById(id).orElseThrow(() ->
                new IllegalArgumentException("Entity of type " + getDbEntityName() + " not found for id: " + id));
    }


    public final List<T> findByIds(List<String> ids) {
        logger.info("Find" + getDbEntityName() + " by ids: " + ids);
        Bson filter = Filters.in("_id", ids);
        return mongoCollection.find(filter).into(new ArrayList<>());
    }

    public void forEachEntity(Consumer<T> consumer) {
        try (MongoCursor<T> cursor = mongoCollection.find().iterator()) {
            while (cursor.hasNext()) {
                T entity = cursor.next();
                consumer.accept(entity);
            }
        }
    }

    public final List<T> findAll() {
        List<T> result = new ArrayList<>();
        for (T entity : mongoCollection.find()) {
            result.add(entity);
        }
        return result;
    }

    public final Optional<T> findSingleBySpecification(LimitSpecification specification) {
        List<T> list = findBySpecification(specification);
        return switch (list.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(list.getFirst());
            default -> throw new IllegalStateException("Expected at most one element, but found: " + list.size());
        };
    }

    public final List<T> findBySpecification(LimitSpecification specification) {
        logger.info("Find" + getDbEntityName() + " by specification: " + specification);

        Bson filter = specification.filter();

        Optional<Collation> collationO = specification.collation();

        List<T> result = new ArrayList<>();
        if (collationO.isEmpty()) {
            for (T entity : mongoCollection.find(filter).sort(specification.sort()).limit(specification.getLimit())) {
                result.add(entity);
            }
        } else {
            for (T entity : mongoCollection.find(filter).collation(collationO.get()).sort(specification.sort()).limit(specification.getLimit())) {
                result.add(entity);
            }
        }
        return result;
    }

    public final DeleteResult deleteBySpecification(LimitSpecification specification) {
        Bson filter = specification.filter();
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        logger.info("Deleted in " + getDbEntityName() + " "
                + deleteResult.getDeletedCount() + " documents by specification: " + specification);
        return deleteResult;
    }

    public final boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public boolean deleteById(String id) {
        DeleteResult deleteResult = mongoCollection.removeById(id);
        if (deleteResult.getDeletedCount() == 1) {
            logger.info(getDbEntityName() + " deleted successfully: " + id);
            return true;
        }
        return false;
    }

    public void deleteAll() {
        DeleteResult deleteResult = mongoCollection.deleteMany(Filters.empty());
        logger.info(getDbEntityName() + " deleted successfully: " + deleteResult.getDeletedCount() + " documents");
    }

    public final long count() {
        return mongoCollection.countDocuments();
    }

}
