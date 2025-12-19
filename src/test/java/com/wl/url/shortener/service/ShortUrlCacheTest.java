package com.wl.url.shortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlCacheTest {

    @Mock
    StringRedisTemplate redis;

    @Mock
    ValueOperations<String, String> valueOps;

    private ShortUrlCache cache;

    @BeforeEach
    void setup() {
        when(redis.opsForValue()).thenReturn(valueOps);
        cache = new ShortUrlCache(redis);
    }

    @Test
    void get_shouldUseCacheKeyPrefix() {
        when(valueOps.get("url:cache:abc")).thenReturn("https://example.com");

        String res = cache.get("abc");

        assertEquals("https://example.com", res);
        verify(valueOps).get("url:cache:abc");
        verifyNoMoreInteractions(redis, valueOps);
    }

    @Test
    void registerHitAndMaybeCache_shouldDoNothingWhenIncrementReturnsNull() {
        when(valueOps.increment("url:hits:abc")).thenReturn(null);

        cache.registerHitAndMaybeCache("abc", "https://full");

        verify(valueOps).increment("url:hits:abc");
        verifyNoMoreInteractions(redis, valueOps);
    }

    @Test
    void registerHitAndMaybeCache_hitsEquals1_shouldSetHitsTtl() {
        when(valueOps.increment("url:hits:abc")).thenReturn(1L);

        cache.registerHitAndMaybeCache("abc", "https://full");

        verify(valueOps).increment("url:hits:abc");
        verify(redis).expire(eq("url:hits:abc"), eq(Duration.ofMinutes(10)));
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        verify(redis, never()).delete(anyString());
    }

    @Test
    void registerHitAndMaybeCache_hitsGreaterThan1_andTtlMissing_shouldResetHitsTtl() {
        when(valueOps.increment("url:hits:abc")).thenReturn(2L);
        when(redis.getExpire("url:hits:abc")).thenReturn(-1L); // sem expiração

        cache.registerHitAndMaybeCache("abc", "https://full");

        verify(valueOps).increment("url:hits:abc");
        verify(redis).getExpire("url:hits:abc");
        verify(redis).expire(eq("url:hits:abc"), eq(Duration.ofMinutes(10)));
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        verify(redis, never()).delete(anyString());
    }

    @Test
    void registerHitAndMaybeCache_hitsGreaterThan1_andTtlOk_shouldNotTouchExpire() {
        when(valueOps.increment("url:hits:abc")).thenReturn(2L);
        when(redis.getExpire("url:hits:abc")).thenReturn(120L); // tem TTL

        cache.registerHitAndMaybeCache("abc", "https://full");

        verify(valueOps).increment("url:hits:abc");
        verify(redis).getExpire("url:hits:abc");
        verify(redis, never()).expire(anyString(), any(Duration.class));
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        verify(redis, never()).delete(anyString());
    }

    @Test
    void registerHitAndMaybeCache_hitsBelowThreshold_shouldNotCache() {
        when(valueOps.increment("url:hits:abc")).thenReturn(19L);
        when(redis.getExpire("url:hits:abc")).thenReturn(120L);

        cache.registerHitAndMaybeCache("abc", "https://full");

        verify(valueOps).increment("url:hits:abc");
        verify(redis).getExpire("url:hits:abc");
        verify(valueOps, never()).set(eq("url:cache:abc"), anyString(), any(Duration.class));
        verify(redis, never()).delete("url:hits:abc");
    }

    @Test
    void registerHitAndMaybeCache_hitsAtThreshold_shouldCacheAndDeleteHitsKey() {
        when(valueOps.increment("url:hits:abc")).thenReturn(20L);
        when(redis.getExpire("url:hits:abc")).thenReturn(120L);

        cache.registerHitAndMaybeCache("abc", "https://full");

        verify(valueOps).increment("url:hits:abc");

        // hits >= 20 -> cache
        verify(valueOps).set(eq("url:cache:abc"), eq("https://full"), eq(Duration.ofHours(2)));
        verify(redis).delete("url:hits:abc");
    }

    @Test
    void registerHitAndMaybeCache_hitsOverThreshold_shouldCacheAndDeleteHitsKey() {
        when(valueOps.increment("url:hits:abc")).thenReturn(50L);
        when(redis.getExpire("url:hits:abc")).thenReturn(120L);

        cache.registerHitAndMaybeCache("abc", "https://full");

        verify(valueOps).set(eq("url:cache:abc"), eq("https://full"), eq(Duration.ofHours(2)));
        verify(redis).delete("url:hits:abc");
    }
}