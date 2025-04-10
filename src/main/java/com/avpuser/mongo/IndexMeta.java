package com.avpuser.mongo;

import com.mongodb.client.model.IndexOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bson.conversions.Bson;

import java.util.Optional;

@ToString
@EqualsAndHashCode
@Data
public class IndexMeta {

    private final String collectionName;

    private final Bson index;

    private final Optional<IndexOptions> indexOptions;

    public IndexMeta(String collectionName, Bson index, IndexOptions indexOptions) {
        this.collectionName = collectionName;
        this.index = index;
        this.indexOptions = Optional.of(indexOptions);
    }

    public IndexMeta(String collectionName, Bson index) {
        this.collectionName = collectionName;
        this.index = index;
        this.indexOptions = Optional.empty();
    }
}
