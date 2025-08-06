package com.avpuser.mongo;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bson.conversions.Bson;

import java.util.Optional;

@Data
@ToString
@EqualsAndHashCode
public abstract class LimitSpecification {

    private final int limit;
    private final int skip;

    public LimitSpecification(int limit) {
        this(limit, 0); // по умолчанию skip = 0 (совместимость)
    }

    public LimitSpecification(int limit, int skip) {
        this.limit = limit;
        this.skip = skip;
    }

    public Bson filter() {
        return Filters.empty();
    }

    public Bson sort() {
        return Sorts.descending();
    }

    public Optional<Collation> collation() {
        return Optional.empty();
    }

}