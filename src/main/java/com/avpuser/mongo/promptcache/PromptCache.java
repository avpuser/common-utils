package com.avpuser.mongo.promptcache;

import com.avpuser.ai.AIModel;
import com.avpuser.mongo.DbEntity;
import com.avpuser.mongo.validate.SkipMongoCollectionValidation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.mongojack.Id;
import org.mongojack.MongoCollection;

@MongoCollection(name = "prompt_cache")
@SkipMongoCollectionValidation
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromptCache extends DbEntity {

    @Id
    private String id;
    private String request;
    private String response;
    private String promptType;
    private AIModel model;

    public PromptCache(String id, String request, String response, String promptType, AIModel model) {
        this.id = id;
        this.request = request;
        this.response = response;
        this.promptType = promptType;
        this.model = model;
    }

    public PromptCache() {
    }

}