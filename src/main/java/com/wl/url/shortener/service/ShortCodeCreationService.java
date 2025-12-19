package com.wl.url.shortener.service;

import com.wl.url.shortener.entity.ShortenerUrl;
import com.wl.url.shortener.exception.impl.RateLimitExceededException;
import com.wl.url.shortener.repository.ShortenerUrlRepository;
import com.wl.url.shortener.utils.ShortCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ShortCodeCreationService {

    private static final int MAX_RETRIES = 8;

    private final ShortenerUrlRepository repository;
    private final ShortCodeGenerator generator;

    public ShortCodeCreationService(ShortenerUrlRepository repository,
                                    @Value("${shortener.node-id:0}") int nodeId) {
        this.repository = repository;
        this.generator = new ShortCodeGenerator(nodeId);
    }

    public String createAndPersist(String fullUrl) {

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String shortCode = generator.next();

                ShortenerUrl entity = ShortenerUrl.builder()
                        .shortcode(shortCode)
                        .fullUrl(fullUrl)
                        .build();

                repository.insert(entity);
                return shortCode;

            } catch (RateLimitExceededException ex) {
                if (attempt == MAX_RETRIES) {
                    throw ex;
                }

            } catch (DataIntegrityViolationException ex) {
                if (attempt == MAX_RETRIES) {
                    throw new IllegalStateException(
                            "Could not generate a unique shortcode", ex
                    );
                }
            }
        }

        throw new IllegalStateException("Could not generate a shortcode");
    }
}