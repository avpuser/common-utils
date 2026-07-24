package com.avpuser.mongo;

import com.avpuser.mongo.encryption.Encrypted;
import com.avpuser.mongo.encryption.EncryptionKeyConfig;
import com.avpuser.mongo.encryption.PiiEncryptionService;
import com.avpuser.mongo.encryption.exception.PiiEncryptionConfigException;
import com.avpuser.test.MockTest;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongojack.Id;
import org.mongojack.MongoCollection;

import java.time.Clock;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * The 3-arg {@link CommonDao} constructor must never silently encrypt data with a made-up key:
 * if the entity declares an {@code @Encrypted} field, it must fail fast instead of falling back to
 * a key that can't survive a restart.
 */
@MockTest
class CommonDaoEncryptedConstructorTest {

    @Mock
    private MongoDatabase database;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private com.mongodb.client.MongoCollection<EncryptedEntity> nativeCollection;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private com.mongodb.client.MongoCollection<PlainEntity> nativePlainCollection;

    private Clock clock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clock = Clock.systemUTC();
        when(database.getCollection(anyString(), eq(EncryptedEntity.class))).thenReturn(nativeCollection);
        when(database.getCollection(anyString(), eq(PlainEntity.class))).thenReturn(nativePlainCollection);
    }

    @Test
    void threeArgConstructor_throwsForEntityWithEncryptedField() {
        PiiEncryptionConfigException ex = assertThrows(PiiEncryptionConfigException.class,
                () -> new CommonDao<>(database, EncryptedEntity.class, clock));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains(EncryptedEntity.class.getName()));
    }

    @Test
    void fourArgConstructor_succeedsForEntityWithEncryptedFieldWhenServiceProvided() {
        PiiEncryptionService service = new PiiEncryptionService(EncryptionKeyConfig.create(
                "test-key", EncryptionKeyConfig.generateRandomAesKeyBase64(), Map.of(),
                EncryptionKeyConfig.generateRandomHmacKeyBase64()));

        assertDoesNotThrow(() -> new CommonDao<>(database, EncryptedEntity.class, clock, service));
    }

    @Test
    void threeArgConstructor_succeedsForEntityWithoutEncryptedField() {
        assertDoesNotThrow(() -> new CommonDao<>(database, PlainEntity.class, clock));
    }

    @MongoCollection(name = "encrypted_entity_ctor_test")
    static class EncryptedEntity extends DbEntity {
        @Id
        private String id;

        @Encrypted(context = "encrypted_entity_ctor_test_v1:secret")
        private String secret;

        @Override
        public String getId() {
            return id;
        }
    }

    @MongoCollection(name = "plain_entity_ctor_test")
    static class PlainEntity extends DbEntity {
        @Id
        private String id;

        private String regular;

        @Override
        public String getId() {
            return id;
        }
    }
}
