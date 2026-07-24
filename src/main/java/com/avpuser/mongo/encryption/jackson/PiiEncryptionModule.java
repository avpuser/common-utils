package com.avpuser.mongo.encryption.jackson;

import com.avpuser.mongo.encryption.PiiEncryptionService;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson module that installs transparent field-level encryption/decryption for
 * {@link com.avpuser.mongo.encryption.Encrypted}-annotated fields on any entity serialized by the
 * {@link com.fasterxml.jackson.databind.ObjectMapper} it's registered on. Register once per
 * {@code ObjectMapper} instance (e.g.
 * one per {@link com.avpuser.mongo.CommonDao}, as built by
 * {@link com.avpuser.mongo.typeconverter.MongoObjectMapperFactory}).
 */
public final class PiiEncryptionModule extends SimpleModule {

    public PiiEncryptionModule(PiiEncryptionService encryptionService) {
        super("PiiEncryptionModule");
        setSerializerModifier(new PiiEncryptionSerializerModifier(encryptionService));
        setDeserializerModifier(new PiiEncryptionDeserializerModifier(encryptionService));
    }
}
