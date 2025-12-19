package com.wl.url.shortener.utils;

import com.wl.url.shortener.exception.impl.RateLimitExceededException;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

public final class ShortCodeGenerator {

    private static final int NODE_BITS = 5;
    private static final int SEQ_BITS = 6;
    private static final int SEQ_MAX = (1 << SEQ_BITS) - 1;
    private static final long CUSTOM_EPOCH_SECONDS = 1735689600L;

    private final int nodeId;
    private final Clock clock;

    private final AtomicInteger seq = new AtomicInteger(0);
    private volatile long lastSecond = -1;

    public ShortCodeGenerator(int nodeId) {
        this(nodeId, Clock.systemUTC());
    }

    public ShortCodeGenerator(int nodeId, Clock clock) {
        if (nodeId < 0 || nodeId >= (1 << NODE_BITS)) {
            throw new IllegalArgumentException("nodeId must be between 0 and 31");
        }
        this.nodeId = nodeId;
        this.clock = clock;
    }

    public String next() {
        long nowSec = currentSecond();

        int s;
        synchronized (this) {
            if (nowSec != lastSecond) {
                lastSecond = nowSec;
                seq.set(0);
            }

            s = seq.getAndIncrement();
            if (s > SEQ_MAX) {
                throw new RateLimitExceededException("Shortcode generation rate exceeded");
            }
        }

        long id =
                (nowSec << (NODE_BITS + SEQ_BITS)) |
                        ((long) nodeId << SEQ_BITS) |
                        (long) s;

        return ShortenerUtils.encode(id);
    }

    private long currentSecond() {
        long sec = (clock.millis() / 1000L) - CUSTOM_EPOCH_SECONDS;
        if (sec < 0) {
            throw new IllegalStateException("Clock moved backwards");
        }
        return sec;
    }
}