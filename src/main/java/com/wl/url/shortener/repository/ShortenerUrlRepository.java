package com.wl.url.shortener.repository;

import com.wl.url.shortener.entity.ShortenerUrl;
import org.springframework.data.cassandra.repository.CassandraRepository;

public interface ShortenerUrlRepository extends CassandraRepository<ShortenerUrl, String> {
}

