package com.avpuser.mongo.typeconverter;

import com.avpuser.mongo.CommonDao;
import com.avpuser.mongo.DbEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Scans all MongoDB entities managed by {@link CommonDao} and rewrites them
 * using the latest serialization logic.
 *
 * <p>This is typically used after updating serialization formats
 * (e.g., storing {@link java.time.Instant} as numbers instead of ISO strings),
 * or after introducing new type converters.</p>
 *
 * <p>For each entity type, this service loads all existing documents from MongoDB,
 * then calls {@link CommonDao#update(DbEntity)} to reserialize and persist the normalized version.</p>
 *
 * <p>Can be safely executed in development or staging environments to fix old documents.
 * Should be used with caution in production environments due to potential high load
 * and impact on data consistency.</p>
 * <p>
 * Example usage:
 * <pre>{@code
 *     MongoEntityFixer fixer = new MongoEntityFixer(allDaos);
 *     fixer.fixAll();
 * }</pre>
 *
 * @see com.avpuser.mongo.CommonDao
 */
public class MongoEntityFixer {

    private static final Logger logger = LogManager.getLogger(MongoEntityFixer.class);

    private final Map<Class<?>, CommonDao<? extends DbEntity>> allDaos;

    public MongoEntityFixer(Map<Class<?>, CommonDao<? extends DbEntity>> allDaos) {
        this.allDaos = allDaos;
    }

    /**
     * Iterates through all DAOs and re-saves each entity to ensure that newly registered
     * serializers/deserializers are applied, fixing outdated stored formats.
     */
    public void fixAllData() {
        for (Map.Entry<Class<?>, CommonDao<? extends DbEntity>> entry : allDaos.entrySet()) {
            Class<?> entityClass = entry.getKey();
            CommonDao<? extends DbEntity> dao = entry.getValue();

            logger.info("Fixing data for entity: {}", entityClass.getSimpleName());

            List<? extends DbEntity> allEntities = dao.findAll();
            int total = allEntities.size();
            int fixed = 0;

            for (DbEntity entity : allEntities) {
                try {
                    // Мы знаем, что dao и entity совместимы по типу, так что подавляем ворнинг
                    @SuppressWarnings("unchecked")
                    CommonDao<DbEntity> castedDao = (CommonDao<DbEntity>) dao;
                    castedDao.update(entity);

                    fixed++;
                } catch (Exception e) {
                    logger.error("Failed to fix entity of type {} with id {}: {}", entityClass.getSimpleName(), entity.getId(), e.getMessage(), e);
                }
            }

            logger.info("Finished fixing {} entities for {} ({} updated)", total, entityClass.getSimpleName(), fixed);
        }

    }
}