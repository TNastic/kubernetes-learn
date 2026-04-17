package com.example.taskmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:testdb",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.redis.host=127.0.0.1",
            "spring.redis.port=6379",
            "spring.redis.password="
        })
class TaskManagerApplicationTests {

    @Test
    void contextLoads() {
    }
}
