package com.wl.url.shortener.utils;

public class ShortenerUtils {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int BASE = 62;

    private ShortenerUtils() {
    }

    public static String encode(long value) {

        if (value < 0) throw new IllegalArgumentException("value must be >= 0");

        if (value == 0) return "0";

        char[] buf = new char[12];

        int i = buf.length;

        while (value > 0) {
            int rem = (int) (value % BASE);
            buf[--i] = ALPHABET[rem];
            value /= BASE;
        }

        return new String(buf, i, buf.length - i);
    }
}
