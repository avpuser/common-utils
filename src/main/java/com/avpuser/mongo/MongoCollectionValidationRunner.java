package com.avpuser.mongo;

import com.avpuser.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MongoCollectionValidationRunner {

    private final Environment environment;

    public MongoCollectionValidationRunner(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void run() {
        MongoCollectionValidator.validate();
    }
}