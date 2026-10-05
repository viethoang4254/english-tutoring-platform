package com.englishlearning;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// This skeleton test deliberately excludes database infrastructure.
// DatabaseConnectivityTests verifies the real datasource separately.
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
class EnglishLearningApplicationTests {

    @Test
    void contextLoads() {
    }
}
