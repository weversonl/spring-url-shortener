package com.wl.url.shortener.exception.handle;

import com.wl.url.shortener.exception.impl.NotFoundException;
import com.wl.url.shortener.exception.impl.RateLimitExceededException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
class TestController {

    @GetMapping("/not-found")
    public ResponseEntity<Void> notFound() {
        throw new NotFoundException("Short URL not found");
    }

    @GetMapping("/rate-limit")
    public ResponseEntity<Void> rateLimit() {
        throw new RateLimitExceededException("Too many requests");
    }

    @GetMapping("/generic")
    public ResponseEntity<Void> generic() {
        throw new RuntimeException("Boom");
    }

    @PostMapping("/validation")
    public ResponseEntity<Void> validation(@Valid @RequestBody ValidationRequest req) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/multi-validation")
    public ResponseEntity<Void> multiValidation(@Valid @RequestBody MultiValidationRequest req) {
        return ResponseEntity.ok().build();
    }

    record ValidationRequest(
            @NotBlank(message = "must not be blank")
            String value
    ) {
    }

    record MultiValidationRequest(
            @NotBlank(message = "must not be blank")
            String value,

            @Size(min = 3, message = "size must be at least 3")
            String name
    ) {
    }
}