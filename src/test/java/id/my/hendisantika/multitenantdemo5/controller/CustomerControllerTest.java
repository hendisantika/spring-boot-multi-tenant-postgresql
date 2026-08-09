package id.my.hendisantika.multitenantdemo5.controller;

import id.my.hendisantika.multitenantdemo5.config.AppTenantContext;
import id.my.hendisantika.multitenantdemo5.repository.CustomerRepository;
import id.my.hendisantika.multitenantdemo5.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the controller through the real filter chain so the X-TenantID header
 * is resolved the same way it is for a live request.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = "spring.docker.compose.enabled=false")
class CustomerControllerTest {

    private static final String TENANT_A = "tenant_a";
    private static final String TENANT_B = "tenant_b";
    private static final String TENANT_HEADER = "X-TenantID";
    private static final String ALICE = """
            {"name":"Alice","email":"alice@example.com"}""";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18.4-alpine3.24");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void resetTenants() {
        tenantService.createTenant(TENANT_A);
        tenantService.createTenant(TENANT_B);
        for (String tenant : new String[]{TENANT_A, TENANT_B}) {
            AppTenantContext.setCurrentTenant(tenant);
            customerRepository.deleteAll();
        }
        AppTenantContext.clear();
    }

    @Test
    void createsAndReadsBackWithinOneTenant() throws Exception {
        mockMvc.perform(post("/customers").header(TENANT_HEADER, TENANT_A)
                        .contentType(MediaType.APPLICATION_JSON).content(ALICE))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void keepsTenantsIsolatedOverHttp() throws Exception {
        mockMvc.perform(post("/customers").header(TENANT_HEADER, TENANT_A)
                .contentType(MediaType.APPLICATION_JSON).content(ALICE)).andExpect(status().isCreated());

        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // The unique index on email is per schema, so the address is free here.
        mockMvc.perform(post("/customers").header(TENANT_HEADER, TENANT_B)
                        .contentType(MediaType.APPLICATION_JSON).content(ALICE))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsDuplicateEmailWithinTheSameTenant() throws Exception {
        mockMvc.perform(post("/customers").header(TENANT_HEADER, TENANT_A)
                .contentType(MediaType.APPLICATION_JSON).content(ALICE)).andExpect(status().isCreated());

        mockMvc.perform(post("/customers").header(TENANT_HEADER, TENANT_A)
                        .contentType(MediaType.APPLICATION_JSON).content(ALICE))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/customers").header(TENANT_HEADER, TENANT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/customers/9999").header(TENANT_HEADER, TENANT_A))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/customers/9999").header(TENANT_HEADER, TENANT_A)
                        .contentType(MediaType.APPLICATION_JSON).content(ALICE))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/customers/9999").header(TENANT_HEADER, TENANT_A))
                .andExpect(status().isNotFound());
    }
}
