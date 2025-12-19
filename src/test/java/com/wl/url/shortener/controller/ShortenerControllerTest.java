package com.wl.url.shortener.controller;

import com.wl.url.shortener.dto.response.ShortenerFullResponse;
import com.wl.url.shortener.dto.response.ShortenerResponse;
import com.wl.url.shortener.exception.handle.GlobalExceptionHandler;
import com.wl.url.shortener.exception.impl.NotFoundException;
import com.wl.url.shortener.exception.impl.RateLimitExceededException;
import com.wl.url.shortener.service.ShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShortenerControllerTest {

    private MockMvc mockMvc;
    private ShortenerService shortenerService;

    @BeforeEach
    void setup() {
        shortenerService = Mockito.mock(ShortenerService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ShortenerController controller = new ShortenerController(shortenerService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void redirect_shouldReturn302AndLocationToOriginalUrl() throws Exception {
        ShortenerFullResponse full = new ShortenerFullResponse();
        full.setUrl("https://example.com/abc");

        when(shortenerService.findByShortUrl("xYz123")).thenReturn(full);

        mockMvc.perform(get("/api/shortener/xYz123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/abc"))
                .andExpect(content().string(""));

        verify(shortenerService).findByShortUrl("xYz123");
        verifyNoMoreInteractions(shortenerService);
    }

    @Test
    void createShortcode_shouldReturn201_LocationAndBodyWithShortUrl_usingRequestHost() throws Exception {
        ShortenerResponse sr = new ShortenerResponse();
        sr.setShortCode("abcd12");

        when(shortenerService.save(eq("https://google.com"))).thenReturn(sr);

        String body = """
                { "url": "https://google.com" }
                """;

        mockMvc.perform(post("/api/shortener")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Host", "meu-dominio.com:8080"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://meu-dominio.com:8080/api/shortener/abcd12"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shortCode").value("abcd12"))
                .andExpect(jsonPath("$.shortUrl").value("http://meu-dominio.com:8080/api/shortener/abcd12"));

        verify(shortenerService).save("https://google.com");
        verifyNoMoreInteractions(shortenerService);
    }

    @Test
    void createShortcode_shouldReturn400_whenValidationFails() throws Exception {
        String body = """
                { "url": "" }
                """;

        mockMvc.perform(post("/api/shortener")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/shortener"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").isString());

        verifyNoInteractions(shortenerService);
    }

    @Test
    void redirect_shouldReturn404_whenServiceThrowsNotFound() throws Exception {
        when(shortenerService.findByShortUrl("nope"))
                .thenThrow(new NotFoundException("Short URL not found"));

        mockMvc.perform(get("/api/shortener/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Short URL not found"))
                .andExpect(jsonPath("$.path").value("/api/shortener/nope"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(shortenerService).findByShortUrl("nope");
        verifyNoMoreInteractions(shortenerService);
    }

    @Test
    void createShortcode_shouldReturn429_whenServiceThrowsRateLimit() throws Exception {
        when(shortenerService.save(anyString()))
                .thenThrow(new RateLimitExceededException("Too many requests"));

        String body = """
                { "url": "https://example.com" }
                """;

        mockMvc.perform(post("/api/shortener")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Too many requests"))
                .andExpect(jsonPath("$.path").value("/api/shortener"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(shortenerService).save("https://example.com");
        verifyNoMoreInteractions(shortenerService);
    }

    @Test
    void createShortcode_shouldReturn500_whenServiceThrowsGeneric() throws Exception {
        when(shortenerService.save(anyString()))
                .thenThrow(new RuntimeException("Boom"));

        String body = """
                { "url": "https://example.com" }
                """;

        mockMvc.perform(post("/api/shortener")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Boom"))
                .andExpect(jsonPath("$.path").value("/api/shortener"));

        verify(shortenerService).save("https://example.com");
        verifyNoMoreInteractions(shortenerService);
    }
}