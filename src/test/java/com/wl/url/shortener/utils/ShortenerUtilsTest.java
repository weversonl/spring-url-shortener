package com.wl.url.shortener.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShortenerUtilsTest {

    @Test
    void encode_shouldThrowExceptionForNegativeValue() {
        assertThrows(IllegalArgumentException.class, () -> ShortenerUtils.encode(-1));
    }

    @Test
    void encode_shouldReturnZeroForZero() {
        assertEquals("0", ShortenerUtils.encode(0));
    }

    @Test
    void encode_shouldEncodeSmallNumbersCorrectly() {
        assertEquals("1", ShortenerUtils.encode(1));
        assertEquals("9", ShortenerUtils.encode(9));
        assertEquals("A", ShortenerUtils.encode(10));
        assertEquals("Z", ShortenerUtils.encode(35));
        assertEquals("a", ShortenerUtils.encode(36));
        assertEquals("z", ShortenerUtils.encode(61));
    }

    @Test
    void encode_shouldEncodeBaseOverflowCorrectly() {
        assertEquals("10", ShortenerUtils.encode(62));
        assertEquals("11", ShortenerUtils.encode(63));
        assertEquals("1z", ShortenerUtils.encode(62 + 61));
    }

    @Test
    void encode_shouldBeDeterministic() {
        long value = 123456789L;

        String first = ShortenerUtils.encode(value);
        String second = ShortenerUtils.encode(value);

        assertEquals(first, second);
    }

    @Test
    void encode_shouldGenerateOnlyBase62Characters() {
        long value = Long.MAX_VALUE;

        String encoded = ShortenerUtils.encode(value);

        assertNotNull(encoded);
        assertFalse(encoded.isBlank());

        for (char c : encoded.toCharArray()) {
            assertTrue(
                    isBase62(c),
                    "Caractere inválido encontrado: " + c
            );
        }
    }

    @Test
    void encode_shouldGenerateUniqueValuesForDifferentInputs() {
        Set<String> values = new HashSet<>();

        for (long i = 0; i < 10_000; i++) {
            String encoded = ShortenerUtils.encode(i);
            assertTrue(values.add(encoded), "Colisão para valor: " + i);
        }
    }

    private boolean isBase62(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z');
    }
}