package com.avpuser.mongo;

import com.avpuser.mongo.exception.EntityNotFoundException;
import com.avpuser.mongo.exception.VersionConflictException;
import com.avpuser.mongo.typeconverter.MongoObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
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
    private final String dbEntityName;

    public CommonDao(MongoDatabase database, Class<T> type, Clock clock) {
        this.type = type;
        this.clock = clock;
        this.dbEntityName = type.getSimpleName();

        ObjectMapper objectMapper = MongoObjectMapperFactory.createObjectMapper();
        this.mongoCollection = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(database, type, UuidRepresentation.STANDARD);
    }

    public final Class<T> getType() {
        return type;
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
        logger.info("{} saved successfully. id={}", dbEntityName, entity.getId());
        return entity.getId();
    }


    private void verifyExistsAndVersionMatches(T entity) {
        Optional<T> dbEntityO = findById(entity.getId());
        if (dbEntityO.isEmpty()) {
            throw new EntityNotFoundException("No " + dbEntityName + " with id: " + entity.getId());
        }
        if (entity.getVersion() != dbEntityO.get().getVersion()) {
            throw new VersionConflictException("Version conflict for " + dbEntityName + " with id: " + entity.getId() +
                    ". Possibly modified concurrently. Expected version: " + entity.getVersion() +
                    ", but found: " + dbEntityO.get().getVersion());
        }
    }

    public final void update(T entity) {
        String id = entity.getId();
        verifyExistsAndVersionMatches(entity);

        Instant now = clock.instant();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);

        long oldVersion = entity.getVersion();
        entity.setVersion(oldVersion + 1);

        UpdateResult result = tryVersionedUpdate(id, oldVersion, entity);

        if (result.getModifiedCount() > 0) {
            logSuccess(id);
            return;
        }

        if (oldVersion > 0) {
            throw new VersionConflictException("Version conflict for " + dbEntityName + " with id: " + id +
                    ". Possibly modified concurrently. Expected version: " + oldVersion);
        }

        result = tryFallbackUpdateForLegacyEntity(id, entity);
        if (result.getModifiedCount() == 0) {
            throw new EntityNotFoundException("No " + dbEntityName + " with id: " + id);
        }

        logger.warn("Fallback used for entity without version: " + id);
        logSuccess(id);
    }

    private UpdateResult tryVersionedUpdate(String id, long version, T entity) {
        return mongoCollection.replaceOne(
                Filters.and(
                        Filters.eq("_id", id),
                        Filters.eq("version", version)
                ),
                entity
        );
    }

    /**
     * Attempts to update the entity using only the ID filter, without version check.
     * This fallback is used for legacy entities that do not have a version field in the database.
     * Should be used cautiously, as it may overwrite concurrent changes.
     */
    private UpdateResult tryFallbackUpdateForLegacyEntity(String id, T entity) {
        return mongoCollection.replaceOne(
                Filters.eq("_id", id),
                entity
        );
    }

    private void logSuccess(String id) {
        logger.info(dbEntityName + " updated successfully. " + id);
    }

    public final Optional<T> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        Optional<T> entity = Optional.ofNullable(mongoCollection.findOneById(id));
        if (entity.isEmpty()) {
            logger.info("Entity of type " + dbEntityName + " not found for id: " + id);
        }
        return entity;
    }

    public T findByIdOrThrow(String id) {
        return findById(id).orElseThrow(() ->
                new IllegalArgumentException("Entity of type " + dbEntityName + " not found for id: " + id));
    }


    public final List<T> findByIds(List<String> ids) {
        logger.info("Find " + dbEntityName + " by ids: " + ids);
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

    public List<T> findAll() {
        List<T> result = new ArrayList<>();
        try (MongoCursor<T> cursor = mongoCollection.find().iterator()) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
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
        logger.info("Find {} by specification: {}", dbEntityName, specification);

        Bson filter = specification.filter();
        Optional<Collation> collationO = specification.collation();

        var findQuery = mongoCollection.find(filter)
                .sort(specification.sort())
                .limit(specification.getLimit());

        if (collationO.isPresent()) {
            findQuery = findQuery.collation(collationO.get());
        }

        List<T> result = new ArrayList<>();
        try (MongoCursor<T> cursor = findQuery.iterator()) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        }

        return result;
    }

    public final DeleteResult deleteBySpecification(LimitSpecification specification) {
        Bson filter = specification.filter();
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        logger.info("Deleted in " + dbEntityName + " "
                + deleteResult.getDeletedCount() + " documents by specification: " + specification);
        return deleteResult;
    }

    public final boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public final long countBySpecification(LimitSpecification specification) {
        return mongoCollection.countDocuments(specification.filter());
    }

    public boolean deleteById(String id) {
        DeleteResult deleteResult = mongoCollection.removeById(id);
        if (deleteResult.getDeletedCount() == 1) {
            logger.info("{} deleted successfully: {}", dbEntityName, id);
            return true;
        } else {
            logger.warn("{} not deleted (possibly not found): {}", dbEntityName, id);
            return false;
        }
    }

    public void deleteAll() {
        DeleteResult deleteResult = mongoCollection.deleteMany(Filters.empty());
        logger.info(dbEntityName + " deleted successfully: " + deleteResult.getDeletedCount() + " documents");
    }

    public final long count() {
        return mongoCollection.countDocuments();
    }

}
