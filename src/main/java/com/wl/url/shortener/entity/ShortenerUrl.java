package com.wl.url.shortener.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("shortner_url")
public class ShortenerUrl {

    @PrimaryKey
    @Column("shortcode")
    private String shortcode;

    @Column("full_url")
    private String fullUrl;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

}
