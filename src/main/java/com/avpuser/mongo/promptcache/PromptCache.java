package com.avpuser.mongo.promptcache;

import com.avpuser.gpt.executor.StringPromptRequest;
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
    private StringPromptRequest request;
    private String response;

    public PromptCache(String id, StringPromptRequest request, String response) {
        this.id = id;
        this.request = request;
        this.response = response;
    }

    public PromptCache() {
    }

}