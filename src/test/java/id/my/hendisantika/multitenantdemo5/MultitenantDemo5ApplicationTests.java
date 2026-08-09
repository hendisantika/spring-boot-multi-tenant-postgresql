package id.my.hendisantika.multitenantdemo5;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the whole context against a throwaway PostgreSQL container so the build
 * does not depend on the compose stack (or on any particular host port) being up.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = "spring.docker.compose.enabled=false")
class MultitenantDemo5ApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18.4-alpine3.24");

    @Test
    void contextLoads() {
    }

}
