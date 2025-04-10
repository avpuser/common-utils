package com.avpuser.env;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Data
public class Environment {

    private final EnvironmentEnum env;

    public Environment(String value) {
        this.env = EnvironmentEnum.fromString(value);
    }
}
