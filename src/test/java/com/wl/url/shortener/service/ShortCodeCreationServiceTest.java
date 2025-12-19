package com.wl.url.shortener.service;

import com.wl.url.shortener.entity.ShortenerUrl;
import com.wl.url.shortener.exception.impl.RateLimitExceededException;
import com.wl.url.shortener.repository.ShortenerUrlRepository;
import com.wl.url.shortener.utils.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortCodeCreationServiceTest {

    @Mock
    ShortenerUrlRepository repository;

    @Mock
    ShortCodeGenerator generator;

    private ShortCodeCreationService service;

    @BeforeEach
    void setup() throws Exception {
        service = new ShortCodeCreationService(repository, 0);
        injectGenerator(service, generator);
    }

    @Test
    void createAndPersist_shouldInsertEntityAndReturnShortCode() {
        when(generator.next()).thenReturn("abc123");

        String res = service.createAndPersist("https://example.com");

        assertEquals("abc123", res);

        ArgumentCaptor<ShortenerUrl> captor = ArgumentCaptor.forClass(ShortenerUrl.class);
        verify(repository).insert(captor.capture());

        ShortenerUrl saved = captor.getValue();
        assertEquals("abc123", saved.getShortcode());
        assertEquals("https://example.com", saved.getFullUrl());

        verify(generator).next();
        verifyNoMoreInteractions(repository, generator);
    }

    @Test
    void createAndPersist_shouldRetryOnRateLimitAndEventuallySucceed() {
        when(generator.next())
                .thenThrow(new RateLimitExceededException("rate"))
                .thenReturn("ok1");

        String res = service.createAndPersist("https://example.com");

        assertEquals("ok1", res);
        verify(generator, times(2)).next();
        verify(repository, times(1)).insert(any(ShortenerUrl.class));
    }

    @Test
    void createAndPersist_shouldThrowRateLimitAfterMaxRetries() {
        when(generator.next()).thenThrow(new RateLimitExceededException("rate"));

        assertThrows(RateLimitExceededException.class,
                () -> service.createAndPersist("https://example.com"));

        verify(generator, times(8)).next();
        verifyNoInteractions(repository); // nunca chegou a inserir
    }

    @Test
    void createAndPersist_shouldRetryOnDuplicateAndEventuallySucceed() {
        when(generator.next()).thenReturn("c1", "c2");

        doThrow(new DataIntegrityViolationException("dup"))
                .doReturn(null)
                .when(repository).insert(any(ShortenerUrl.class));

        String res = service.createAndPersist("https://example.com");

        assertEquals("c2", res);

        verify(generator, times(2)).next();
        verify(repository, times(2)).insert(any(ShortenerUrl.class));
    }

    @Test
    void createAndPersist_shouldThrowIllegalStateAfterMaxRetriesOnDuplicates() {
        when(generator.next()).thenReturn("c");

        doThrow(new DataIntegrityViolationException("dup"))
                .when(repository).insert(any(ShortenerUrl.class));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.createAndPersist("https://example.com"));

        assertTrue(ex.getMessage().contains("Could not generate a unique shortcode"));
        assertNotNull(ex.getCause());
        assertInstanceOf(DataIntegrityViolationException.class, ex.getCause());

        verify(generator, times(8)).next();
        verify(repository, times(8)).insert(any(ShortenerUrl.class));
    }

    private static void injectGenerator(ShortCodeCreationService target, ShortCodeGenerator newGenerator) throws Exception {
        Field f = ShortCodeCreationService.class.getDeclaredField("generator");
        f.setAccessible(true);
        f.set(target, newGenerator);
    }
}