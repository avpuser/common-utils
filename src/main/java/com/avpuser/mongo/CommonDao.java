package com.avpuser.mongo;

import com.avpuser.mongo.exception.DuplicateKeyException;
import com.avpuser.mongo.exception.EntityNotFoundException;
import com.avpuser.mongo.exception.VersionConflictException;
import com.avpuser.mongo.typeconverter.MongoObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
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
import java.util.Map;
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
        try {
            mongoCollection.insert(entity);
            logger.info("{} saved successfully. id={}", dbEntityName, entity.getId());
            return entity.getId();
        } catch (MongoBulkWriteException e) {
            List<BulkWriteError> errors = e.getWriteErrors();
            boolean isDuplicate = errors != null && errors.stream()
                    .anyMatch(err -> ErrorCategory.fromErrorCode(err.getCode()) == ErrorCategory.DUPLICATE_KEY);

            if (isDuplicate) {
                String message = String.format(
                        "Duplicate key error in collection '%s': document with id '%s' already exists",
                        dbEntityName,
                        entity.getId()
                );
                logger.warn(message);
                throw new DuplicateKeyException(message, e);
            }

            // If it’s not a duplicate, rethrow the original exception.
            throw e;
        }
    }


    private void verifyExistsAndVersionMatches(T entity) {
        Optional<T> dbEntityO = findById(entity.getId());
        if (dbEntityO.isEmpty()) {
            throw new EntityNotFoundException("No " + dbEntityName + " with id: " + entity.getId());
        }
        if (entity.getVersion() != dbEntityO.get().getVersion()) {
            throw new VersionConflictException("Version conflict for " + dbEntityName +
                    ". Possibly modified concurrently.");
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

    /**
     * Finds entities with optional filtering, sorting, and pagination.
     * <p>
     * ⚠ WARNING: This method is best suited for admin panels, dashboards, and exploratory tools.
     * Avoid using it in performance-critical production paths unless appropriate indexes exist for the filters and sorting.
     * </p>
     *
     * @param limit      Maximum number of documents to return. Must be > 0.
     * @param skip       Number of documents to skip for pagination. Use 0 for the first page.
     * @param filters    A map of field names to exact values to filter by. Combined with logical AND. Can be null or empty.
     * @param sortFields A map of field names to sort order (true = ascending, false = descending). Can be null or empty.
     * @return List of matched and sorted documents according to provided parameters.
     */
    public List<T> findWithFiltersAndSort(int limit, int skip,
                                          Map<String, Object> filters,
                                          Map<String, Boolean> sortFields) {
        logger.info("Find {} with limit={}, skip={}, filters={}, sortFields={}",
                dbEntityName, limit, skip, filters, sortFields);

        // 1. Validate limit
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be > 0");
        }

        // 2. Build filter
        Bson filter = Filters.empty();
        if (filters != null && !filters.isEmpty()) {
            List<Bson> filterList = new ArrayList<>();
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                filterList.add(Filters.eq(entry.getKey(), entry.getValue()));
            }
            filter = Filters.and(filterList);
        }

        // 3. Build sort
        Bson sort = null;
        if (sortFields != null && !sortFields.isEmpty()) {
            List<Bson> sortList = new ArrayList<>();
            for (Map.Entry<String, Boolean> entry : sortFields.entrySet()) {
                sortList.add(entry.getValue()
                        ? Sorts.ascending(entry.getKey())
                        : Sorts.descending(entry.getKey()));
            }
            sort = Sorts.orderBy(sortList);
        }

        // 4. Build query
        var query = mongoCollection.find(filter)
                .limit(limit)
                .skip(skip);

        if (sort != null) {
            query = query.sort(sort);
        }

        // 5. Fetch results
        List<T> result = new ArrayList<>();
        try (MongoCursor<T> cursor = query.iterator()) {
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
        if (!specification.getClass().getSimpleName().equals("LabReportByStatusSpecification")) {
            logger.info("Find {} by specification: {}", dbEntityName, specification);
        } else {
            logger.debug("Find {} by specification: {}", dbEntityName, specification);
        }

        Bson filter = specification.filter();
        Optional<Collation> collationO = specification.collation();

        var findQuery = mongoCollection.find(filter)
                .sort(specification.sort())
                .limit(specification.getLimit())
                .skip(specification.getSkip());

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
