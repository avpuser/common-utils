package com.avpuser.mongo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;

@ToString
@EqualsAndHashCode
@Data
public abstract class DbEntity {

    private Instant createdAt;

    private Instant updatedAt;

    public abstract String getId();

}
