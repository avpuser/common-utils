package ai;

import com.avpuser.ai.AIModel;
import com.avpuser.mongo.promptcache.PromptCacheKeyUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PromptCacheKeyUtilsTest {

    private static final AIModel MODEL = AIModel.GPT_4;
    private static final String PROMPT_TYPE = "type";

    @Test
    void sameContentDifferentWhitespaces_shouldProduceSameHash() {
        String userPrompt1 = "Hello   world";
        String userPrompt2 = "  Hello \t world  ";
        String systemPrompt1 = "System\n\ncontext";
        String systemPrompt2 = "System   context";

        String hash1 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt1, systemPrompt1, MODEL);
        String hash2 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt2, systemPrompt2, MODEL);

        assertEquals(hash1, hash2);
    }

    @Test
    void sameContentDifferentCases_shouldProduceSameHash() {
        String userPrompt1 = "Hello World";
        String userPrompt2 = "hello world";
        String systemPrompt1 = "System Context";
        String systemPrompt2 = "system context";

        String hash1 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt1, systemPrompt1, MODEL);
        String hash2 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt2, systemPrompt2, MODEL);

        assertEquals(hash1, hash2);
    }

    @Test
    void nonBreakingSpaces_shouldBeNormalized() {
        String userPrompt1 = "Hello\u00A0World"; // non-breaking space
        String userPrompt2 = "Hello World";
        String systemPrompt = "Context";

        String hash1 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt1, systemPrompt, MODEL);
        String hash2 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt2, systemPrompt, MODEL);

        assertEquals(hash1, hash2);
    }

    @Test
    void nullAndEmptyValuesHandledSafely() {
        String hash1 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, null, null, MODEL);
        String hash2 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, "", "", MODEL);
        String hash3 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, " ", " ", MODEL);

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @Test
    void completelyDifferentPrompts_shouldProduceDifferentHashes() {
        String hash1 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, "prompt A", "system A", MODEL);
        String hash2 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, "prompt B", "system B", MODEL);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void differentModelNames_shouldProduceDifferentHashes() {
        String prompt = "Hello";
        String context = "World";

        String hash1 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, prompt, context, AIModel.GPT_4);
        String hash2 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, prompt, context, AIModel.GPT_4O_MINI);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void differentPromptTypes_shouldProduceDifferentHashes() {
        String userPrompt = "Same";
        String systemPrompt = "Same";

        String hash1 = PromptCacheKeyUtils.buildHashKey("typeA", userPrompt, systemPrompt, MODEL);
        String hash2 = PromptCacheKeyUtils.buildHashKey("typeB", userPrompt, systemPrompt, MODEL);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void carriageReturnsShouldBeNormalized() {
        String userPrompt1 = "Hello\r\nWorld";
        String userPrompt2 = "Hello\nWorld";
        String systemPrompt = "Sys";

        String hash1 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt1, systemPrompt, MODEL);
        String hash2 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt2, systemPrompt, MODEL);

        assertEquals(hash1, hash2);
    }

    @Test
    void emSpacesShouldBeNormalizedToRegularSpaces() {
        String userPrompt1 = "Hello\u2007World"; // figure space
        String userPrompt2 = "Hello World";
        String systemPrompt = "System";

        String hash1 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt1, systemPrompt, MODEL);
        String hash2 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt2, systemPrompt, MODEL);

        assertEquals(hash1, hash2);
    }

    @Test
    void excessiveInternalSpacesShouldBeCollapsed() {
        String userPrompt1 = "This   is  a    test";
        String userPrompt2 = "This is a test";
        String systemPrompt = "OK";

        String hash1 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt1, systemPrompt, MODEL);
        String hash2 = PromptCacheKeyUtils.buildHashKey(PROMPT_TYPE, userPrompt2, systemPrompt, MODEL);

        assertEquals(hash1, hash2);
    }
}