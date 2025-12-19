package com.wl.url.shortener.service;

import com.wl.url.shortener.dto.response.ShortenerFullResponse;
import com.wl.url.shortener.dto.response.ShortenerResponse;
import com.wl.url.shortener.entity.ShortenerUrl;
import com.wl.url.shortener.exception.impl.NotFoundException;
import com.wl.url.shortener.repository.ShortenerUrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShortenerService {

    private final ShortenerUrlRepository repository;
    private final ShortUrlCache cache;
    private final ShortCodeCreationService creationService;

    public ShortenerService(ShortenerUrlRepository repository,
                            ShortUrlCache cache,
                            ShortCodeCreationService creationService) {
        this.repository = repository;
        this.cache = cache;
        this.creationService = creationService;
    }

    public ShortenerResponse save(String fullUrl) {
        if (!StringUtils.hasText(fullUrl)) {
            throw new IllegalArgumentException("url must not be null/blank");
        }
        String shortCode = creationService.createAndPersist(fullUrl);
        return new ShortenerResponse(shortCode, null);
    }

    public ShortenerFullResponse findByShortUrl(String shortUrl) {
        if (!StringUtils.hasText(shortUrl)) {
            throw new IllegalArgumentException("shortUrl must not be null/blank");
        }

        String cached = cache.get(shortUrl);
        if (cached != null) {
            return new ShortenerFullResponse(cached);
        }

        String fullUrl = repository.findById(shortUrl)
                .map(ShortenerUrl::getFullUrl)
                .orElseThrow(() -> new NotFoundException("Short URL not found"));

        cache.registerHitAndMaybeCache(shortUrl, fullUrl);

        return new ShortenerFullResponse(fullUrl);
    }
}