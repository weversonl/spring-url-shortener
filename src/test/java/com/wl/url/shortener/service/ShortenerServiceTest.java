package com.wl.url.shortener.service;

import com.wl.url.shortener.dto.response.ShortenerFullResponse;
import com.wl.url.shortener.dto.response.ShortenerResponse;
import com.wl.url.shortener.entity.ShortenerUrl;
import com.wl.url.shortener.exception.impl.NotFoundException;
import com.wl.url.shortener.repository.ShortenerUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortenerServiceTest {

    @Mock
    ShortenerUrlRepository repository;

    @Mock
    ShortUrlCache cache;

    @Mock
    ShortCodeCreationService creationService;

    private ShortenerService service;

    @BeforeEach
    void setup() {
        service = new ShortenerService(repository, cache, creationService);
    }

    @Test
    void save_shouldThrowWhenUrlNull() {
        assertThrows(IllegalArgumentException.class, () -> service.save(null));
        verifyNoInteractions(repository, cache, creationService);
    }

    @Test
    void save_shouldThrowWhenUrlBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.save("   "));
        verifyNoInteractions(repository, cache, creationService);
    }

    @Test
    void save_shouldReturnShortCodeFromCreationService() {
        when(creationService.createAndPersist("https://example.com")).thenReturn("abc123");

        ShortenerResponse res = service.save("https://example.com");

        assertNotNull(res);
        assertEquals("abc123", res.getShortCode());

        verify(creationService).createAndPersist("https://example.com");
        verifyNoInteractions(repository, cache);
    }

    @Test
    void findByShortUrl_shouldThrowWhenShortUrlNull() {
        assertThrows(IllegalArgumentException.class, () -> service.findByShortUrl(null));
        verifyNoInteractions(repository, cache, creationService);
    }

    @Test
    void findByShortUrl_shouldThrowWhenShortUrlBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.findByShortUrl("  "));
        verifyNoInteractions(repository, cache, creationService);
    }

    @Test
    void findByShortUrl_shouldReturnCachedValueAndNotHitRepository() {
        when(cache.get("abc")).thenReturn("https://cached.com");

        ShortenerFullResponse res = service.findByShortUrl("abc");

        assertNotNull(res);
        assertEquals("https://cached.com", res.getUrl());

        verify(cache).get("abc");
        verifyNoInteractions(repository);
        verify(cache, never()).registerHitAndMaybeCache(anyString(), anyString());
        verifyNoInteractions(creationService);
    }

    @Test
    void findByShortUrl_cacheMiss_shouldFetchFromRepository_andRegisterHit() {
        when(cache.get("abc")).thenReturn(null);

        ShortenerUrl entity = ShortenerUrl.builder()
                .shortcode("abc")
                .fullUrl("https://db.com")
                .build();

        when(repository.findById("abc")).thenReturn(Optional.of(entity));

        ShortenerFullResponse res = service.findByShortUrl("abc");

        assertNotNull(res);
        assertEquals("https://db.com", res.getUrl());

        verify(cache).get("abc");
        verify(repository).findById("abc");
        verify(cache).registerHitAndMaybeCache("abc", "https://db.com");
        verifyNoInteractions(creationService);
    }

    @Test
    void findByShortUrl_cacheMiss_andNotFound_shouldThrowNotFoundException() {
        when(cache.get("abc")).thenReturn(null);
        when(repository.findById("abc")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findByShortUrl("abc"));

        verify(cache).get("abc");
        verify(repository).findById("abc");
        verify(cache, never()).registerHitAndMaybeCache(anyString(), anyString());
        verifyNoInteractions(creationService);
    }
}