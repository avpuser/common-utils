package com.avpuser.mongo.index;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MongoIndexManager {

    private static final Logger logger = LogManager.getLogger(MongoIndexManager.class);

    private final MongoDatabase database;

    private final List<IndexMeta> indexMetaList;

    public MongoIndexManager(MongoDatabase database) {
        this.database = database;
        this.indexMetaList = IndexMetaUtil.generateAllIndexMeta();
    }

    public Map<IndexMeta, String> createIndexes() {
        Map<IndexMeta, String> names = new HashMap<>();
        for (IndexMeta indexMeta : indexMetaList) {
            names.put(indexMeta, createIndex(indexMeta));
        }
        return names;
    }

    public String createIndex(IndexMeta indexMeta) {
        MongoCollection<Document> collection = database.getCollection(indexMeta.getCollectionName());
        Bson index = indexMeta.getIndex();
        Optional<IndexOptions> indexOptions = indexMeta.getIndexOptions();
        String res = indexOptions.isEmpty()
                ? collection.createIndex(index)
                : collection.createIndex(index, indexOptions.get());
        logger.info("Created index: " + indexMeta.getCollectionName() + "_" + res);
        return res;
    }

    public boolean indexExists(String collectionName, String indexName) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        for (Document idx : collection.listIndexes()) {
            if (indexName.equals(idx.get("name"))) {
                return true;
            }
        }
        return false;
    }

    public void dropIndex(String collectionName, String indexName) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.dropIndex(indexName);
    }


}
