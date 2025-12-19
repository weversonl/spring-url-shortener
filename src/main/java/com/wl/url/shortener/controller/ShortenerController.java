package com.wl.url.shortener.controller;

import com.wl.url.shortener.dto.request.ShortenerRequest;
import com.wl.url.shortener.dto.response.ShortenerFullResponse;
import com.wl.url.shortener.dto.response.ShortenerResponse;
import com.wl.url.shortener.service.ShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/shortener")
@RequiredArgsConstructor
public class ShortenerController {

    private final ShortenerService shortenerService;

    @GetMapping("/{shortcode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortcode) {

        ShortenerFullResponse response = shortenerService.findByShortUrl(shortcode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(response.getUrl()))
                .build();
    }

    @PostMapping
    public ResponseEntity<ShortenerResponse> createShortcode(@Valid @RequestBody ShortenerRequest request,
                                                             HttpServletRequest httpRequest) {

        ShortenerResponse response = shortenerService.save(request.getUrl());

        URI location = ServletUriComponentsBuilder
                .fromRequest(httpRequest)
                .path("/{shortcode}")
                .buildAndExpand(response.getShortCode())
                .toUri();

        response.setShortUrl(location.toString());

        return ResponseEntity
                .created(location)
                .body(response);
    }

}
