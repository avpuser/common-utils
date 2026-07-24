package com.avpuser.mongo.encryption;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link String} field of a {@link com.avpuser.mongo.DbEntity} as sensitive: it is
 * stored encrypted (AES-256-GCM) in MongoDB and transparently decrypted back to plaintext by
 * {@link com.avpuser.mongo.CommonDao} on read.
 * <p>
 * {@link #context()} is used both as AEAD Additional Authenticated Data (so ciphertext cannot be
 * copied into another field/collection) and, when {@link #lookupField()} is set, as the HMAC
 * context for the blind index. It must be an explicit, stable string chosen by the developer -
 * never derived automatically from the Mongo collection name or the Java class/field name, since
 * either can be renamed later without anyone noticing the encrypted data has become undecryptable.
 * Once set for a field that has data in production, it must never change.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Encrypted {

    /**
     * Stable AAD/HMAC context for this field, e.g. {@code "user_v2:contactEmail"}. Must be
     * non-blank; validated eagerly when the owning entity type is first introspected.
     */
    String context();

    /**
     * Name of a sibling {@link String} field on the same entity that stores the HMAC-SHA-256
     * blind index (lookup value) computed from this field's plaintext, enabling exact-match
     * search without decrypting the whole collection. Empty (default) means no lookup index is
     * maintained for this field.
     */
    String lookupField() default "";
}
