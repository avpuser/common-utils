package com.avpuser.mongo.encryption.jackson;

import com.avpuser.mongo.encryption.Encrypted;
import com.avpuser.mongo.encryption.EncryptionKeyConfig;
import com.avpuser.mongo.encryption.PiiEncryptionService;
import com.avpuser.mongo.encryption.exception.UnknownEncryptionKeyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link PiiEncryptionModule} through plain Jackson (de)serialization. MongoJack's
 * {@code JacksonMongoCollection} uses the very same {@code ObjectMapper} machinery to talk to the
 * BSON codec, so a JSON round-trip here exercises the identical bean-property-writer/deserializer
 * wiring that {@link com.avpuser.mongo.CommonDao} relies on against a real MongoDB - without
 * needing a live database for this test.
 */
class PiiEncryptionModuleTest {

    private static final String CONTEXT = "test_entity_v1:secret";

    private ObjectMapper mapper;
    private PiiEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        EncryptionKeyConfig config = EncryptionKeyConfig.create(
                "pii-v1", EncryptionKeyConfig.generateRandomAesKeyBase64(), Map.of(),
                EncryptionKeyConfig.generateRandomHmacKeyBase64());
        encryptionService = new PiiEncryptionService(config);
        mapper = new ObjectMapper();
        // Matches MongoObjectMapperFactory: let our typed decryption exceptions propagate unwrapped.
        mapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.WRAP_EXCEPTIONS);
        mapper.registerModule(new PiiEncryptionModule(encryptionService));
    }

    static class SampleEntity {
        @Encrypted(context = CONTEXT, lookupField = "secretLookup")
        private String secret;
        private String secretLookup;
        private String regular;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getSecretLookup() {
            return secretLookup;
        }

        public void setSecretLookup(String secretLookup) {
            this.secretLookup = secretLookup;
        }

        public String getRegular() {
            return regular;
        }

        public void setRegular(String regular) {
            this.regular = regular;
        }
    }

    static class NoEncryptedFieldsEntity {
        private String plain;

        public String getPlain() {
            return plain;
        }

        public void setPlain(String plain) {
            this.plain = plain;
        }
    }

    @Test
    void serialization_encryptsFieldAndPopulatesLookup() throws Exception {
        SampleEntity entity = new SampleEntity();
        entity.setSecret("hello world");
        entity.setRegular("untouched");

        String json = mapper.writeValueAsString(entity);
        Map<?, ?> asMap = mapper.readValue(json, Map.class);

        assertNotEquals("hello world", asMap.get("secret"));
        assertTrue(((String) asMap.get("secret")).startsWith("msenc:v1:pii-v1:"));
        assertEquals("untouched", asMap.get("regular"));
        assertNotNull(asMap.get("secretLookup"));
        assertEquals(encryptionService.computeLookup("hello world", CONTEXT), asMap.get("secretLookup"));
    }

    @Test
    void serializationDoesNotMutateOriginalObject() throws Exception {
        SampleEntity entity = new SampleEntity();
        entity.setSecret("hello world");

        mapper.writeValueAsString(entity);

        assertEquals("hello world", entity.getSecret());
        assertNull(entity.getSecretLookup()); // never touched by us either
    }

    @Test
    void deserialization_decryptsBackToPlaintext() throws Exception {
        SampleEntity original = new SampleEntity();
        original.setSecret("hello world");
        original.setRegular("untouched");

        String json = mapper.writeValueAsString(original);
        SampleEntity roundTripped = mapper.readValue(json, SampleEntity.class);

        assertEquals("hello world", roundTripped.getSecret());
        assertEquals("untouched", roundTripped.getRegular());
    }

    @Test
    void nullSecret_staysNull_noLookupComputed() throws Exception {
        SampleEntity entity = new SampleEntity();
        entity.setSecret(null);

        String json = mapper.writeValueAsString(entity);
        Map<?, ?> asMap = mapper.readValue(json, Map.class);

        assertNull(asMap.get("secret"));
        assertNull(asMap.get("secretLookup"));

        SampleEntity roundTripped = mapper.readValue(json, SampleEntity.class);
        assertNull(roundTripped.getSecret());
    }

    @Test
    void savingAlreadyEncryptedEntity_doesNotDoubleEncrypt() throws Exception {
        SampleEntity original = new SampleEntity();
        original.setSecret("hello world");
        String json1 = mapper.writeValueAsString(original);

        // Read it back (decrypts to plaintext in memory) and "save" again.
        SampleEntity readBack = mapper.readValue(json1, SampleEntity.class);
        assertEquals("hello world", readBack.getSecret());

        String json2 = mapper.writeValueAsString(readBack);
        SampleEntity roundTripped = mapper.readValue(json2, SampleEntity.class);
        assertEquals("hello world", roundTripped.getSecret());
    }

    @Test
    void entityWithoutEncryptedFields_isCompletelyUnaffected() throws Exception {
        NoEncryptedFieldsEntity entity = new NoEncryptedFieldsEntity();
        entity.setPlain("just plain text");

        String json = mapper.writeValueAsString(entity);
        assertTrue(json.contains("just plain text"));

        NoEncryptedFieldsEntity roundTripped = mapper.readValue(json, NoEncryptedFieldsEntity.class);
        assertEquals("just plain text", roundTripped.getPlain());
    }

    @Test
    void readingRecordEncryptedWithUnknownKeyId_throwsControlledException() {
        String foreignJson = "{\"secret\":\"msenc:v1:some-foreign-key:"
                + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[12]) + ":"
                + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32])
                + "\",\"secretLookup\":null,\"regular\":\"x\"}";

        assertThrows(UnknownEncryptionKeyException.class,
                () -> mapper.readValue(foreignJson, SampleEntity.class));
    }
}
