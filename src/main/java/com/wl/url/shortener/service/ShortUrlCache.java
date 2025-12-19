package com.wl.url.shortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ShortUrlCache {

    private static final Duration CACHE_TTL = Duration.ofHours(2);
    private static final Duration HITS_TTL = Duration.ofMinutes(10);
    private static final long HOT_THRESHOLD = 20;

    private final StringRedisTemplate redis;

    public ShortUrlCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String get(String shortUrl) {
        return redis.opsForValue().get(cacheKey(shortUrl));
    }

    public void registerHitAndMaybeCache(String shortUrl, String fullUrl) {
        String hk = hitsKey(shortUrl);

        Long hits = redis.opsForValue().increment(hk);
        if (hits == null) return;

        if (hits == 1) {
            redis.expire(hk, HITS_TTL);
        } else {
            Long ttl = redis.getExpire(hk);
            if (ttl != null && ttl < 0) {
                redis.expire(hk, HITS_TTL);
            }
        }

        if (hits >= HOT_THRESHOLD) {
            redis.opsForValue().set(cacheKey(shortUrl), fullUrl, CACHE_TTL);
            redis.delete(hk);
        }
    }

    private static String cacheKey(String shortUrl) {
        return "url:cache:" + shortUrl;
    }

    private static String hitsKey(String shortUrl) {
        return "url:hits:" + shortUrl;
    }
}