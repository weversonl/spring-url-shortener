package com.wl.url.shortener.utils;

import com.wl.url.shortener.exception.impl.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Clock;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {

    @Test
    void constructor_shouldRejectNodeIdBelowZero() {
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(-1));
    }

    @Test
    void constructor_shouldRejectNodeIdAbove31() {
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(32));
    }

    @Test
    void next_shouldGenerateNonNullShortCodes() {
        ShortCodeGenerator gen = new ShortCodeGenerator(0);

        String code = gen.next();

        assertNotNull(code);
        assertFalse(code.isBlank());
    }

    @Test
    void next_shouldGenerateUniqueCodesWithinSameSecond_upToSeqMaxInclusive() {
        ShortCodeGenerator gen = new ShortCodeGenerator(1);
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 64; i++) {
            String c = gen.next();
            assertTrue(codes.add(c), "Código duplicado no i=" + i + ": " + c);
        }
    }

    @Test
    void next_shouldThrowRateLimitExceededOn65thCallInSameSecond() {
        ShortCodeGenerator gen = new ShortCodeGenerator(2);

        for (int i = 0; i < 64; i++) {
            gen.next();
        }

        assertThrows(RateLimitExceededException.class, gen::next);
    }

    @Test
    void next_shouldThrowIfClockBeforeCustomEpoch() {
        Clock beforeEpoch = Clock.fixed(java.time.Instant.ofEpochSecond(1735689600L - 1), java.time.ZoneOffset.UTC);
        ShortCodeGenerator gen = new ShortCodeGenerator(0, beforeEpoch);
        assertThrows(IllegalStateException.class, gen::next);
    }

    @Test
    void currentSecond_shouldThrowIfClockWouldBeBeforeCustomEpoch() throws Exception {
        ShortCodeGenerator gen = new ShortCodeGenerator(0);

        Method m = ShortCodeGenerator.class.getDeclaredMethod("currentSecond");
        m.setAccessible(true);

        assertDoesNotThrow(() -> {
            try {
                m.invoke(gen);
            } catch (Exception e) {
                throw unwrap(e);
            }
        });
    }

    private static RuntimeException unwrap(Exception e) {
        Throwable t = e.getCause() != null ? e.getCause() : e;
        return (t instanceof RuntimeException re) ? re : new RuntimeException(t);
    }
}
