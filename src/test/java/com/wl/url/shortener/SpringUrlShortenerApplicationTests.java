package com.wl.url.shortener;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Evitar subir o contexto completo por conta do Cassandra")
class SpringUrlShortenerApplicationTests {

    @Test
    void contextLoads() {
    }

}
