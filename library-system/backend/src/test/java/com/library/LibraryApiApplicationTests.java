package com.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local-h2")
class LibraryApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
