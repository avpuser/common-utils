package com.avpuser.mongo;

import com.mongodb.client.result.DeleteResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.bson.conversions.Bson;

/**
 * Full entity reads and writes use the configured Jackson mapper and therefore
 * support transparent {@code @Encrypted} field encryption and decryption.
 * Raw MongoDB operations such as aggregation, distinct, BSON filters, and
 * partial updates operate on stored ciphertext and require explicit handling
 * of encrypted and lookup fields.
 */
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

    public final long countBySpecification(LimitSpecification specification) {
        return dao.countBySpecification(specification);
    }

    public final boolean existsBySpecification(LimitSpecification specification) {
        return countBySpecification(specification) > 0;
    }

    public final Optional<T> findSingleBySpecification(LimitSpecification specification) {
        return dao.findSingleBySpecification(specification);
    }

    public List<T> findWithFiltersAndSort(int limit, int skip, Map<String, Object> filters, Map<String, Boolean> sortFields) {
        return dao.findWithFiltersAndSort(limit, skip, filters, sortFields);
    }

    public List<T> findWithBsonFilterAndSort(int limit, int skip, org.bson.conversions.Bson filter, Map<String, Boolean> sortFields) {
        return dao.findWithBsonFilterAndSort(limit, skip, filter, sortFields);
    }

    public long countWithBsonFilter(org.bson.conversions.Bson filter) {
        return dao.countWithBsonFilter(filter);
    }

    /**
     * Executes a raw MongoDB {@code distinct} query.
     * <p>
     * This operation does not pass returned values through entity-level Jackson
     * deserialization. For fields annotated with {@code @Encrypted}, MongoDB stores
     * randomized ciphertext, therefore:
     * <ul>
     *     <li>the returned values will be ciphertext, not plaintext;</li>
     *     <li>distinct results are not meaningful because equal plaintext values
     *     may have different ciphertexts;</li>
     *     <li>filtering by plaintext against an encrypted field will not work.</li>
     * </ul>
     * Use the corresponding lookup field for supported exact-match searches.
     * Do not use this method to read or group encrypted field values.
     */
    public <R> List<R> distinct(String fieldName, Bson filter, Class<R> resultClass) {
        return dao.distinct(fieldName, filter, resultClass);
    }

    /**
     * Executes a raw MongoDB aggregation pipeline.
     * <p>
     * Aggregation stages operate on the physical BSON representation stored in
     * MongoDB and do not automatically understand {@code @Encrypted} fields.
     * Encrypted fields contain randomized ciphertext, not plaintext.
     * <p>
     * As a result:
     * <ul>
     *     <li>{@code $match} against plaintext encrypted values will not work;</li>
     *     <li>{@code $group}, {@code $sort}, and {@code $distinct}-like operations
     *     on encrypted fields are not semantically meaningful;</li>
     *     <li>projected encrypted values remain ciphertext unless the final result
     *     is explicitly deserialized as the complete entity through the configured
     *     Jackson mapper.</li>
     * </ul>
     * Use lookup fields for supported exact-match filtering and avoid projecting
     * encrypted values into raw aggregation results.
     */
    public List<org.bson.Document> runAggregation(List<Bson> pipeline) {
        return dao.runAggregation(pipeline);
    }

}
