package com.wl.url.shortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.cassandra.config.EnableCassandraAuditing;

@SpringBootApplication
@EnableCassandraAuditing
public class SpringUrlShortenerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringUrlShortenerApplication.class, args);
    }

}
