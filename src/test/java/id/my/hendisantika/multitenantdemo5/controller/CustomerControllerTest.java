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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    void keepsTenantsIsolatedOverHttp() throws Exception {
        mockMvc.perform(post("/customers").header(TENANT_HEADER, TENANT_A)
                .contentType(MediaType.APPLICATION_JSON).content(ALICE)).andExpect(status().isCreated());

        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));

        // The unique index on email is per schema, so the address is free here.
        mockMvc.perform(post("/customers").header(TENANT_HEADER, TENANT_B)
                        .contentType(MediaType.APPLICATION_JSON).content(ALICE))
                .andExpect(status().isCreated());
    }

    @Test
    void pagesAndSortsTheList() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/customers").header(TENANT_HEADER, TENANT_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Customer %02d\",\"email\":\"c%d@example.com\"}".formatted(i, i)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A)
                        .param("page", "0").param("size", "2").param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name", is("Customer 01")))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$.page.totalElements", is(5)))
                .andExpect(jsonPath("$.page.totalPages", is(3)));

        // Last page holds the remainder.
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A)
                        .param("page", "2").param("size", "2").param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Customer 05")));

        // Descending sort flips the first row.
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A)
                        .param("size", "2").param("sort", "name,desc"))
                .andExpect(jsonPath("$.content[0].name", is("Customer 05")));
    }

    private void create(String tenant, String name, String email) throws Exception {
        String body = email == null
                ? "{\"name\":\"%s\"}".formatted(name)
                : "{\"name\":\"%s\",\"email\":\"%s\"}".formatted(name, email);
        mockMvc.perform(post("/customers").header(TENANT_HEADER, tenant)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
    }

    private void seedSearchFixtures() throws Exception {
        create(TENANT_A, "Alice Anderson", "alice@example.com");
        create(TENANT_A, "Bob Brown", "bob@test.org");
        create(TENANT_A, "50% Off Ltd", "promo@deals.com");
        create(TENANT_A, "Under_score Co", "under@deals.com");
        create(TENANT_A, "NoEmail Person", null);
    }

    @Test
    void searchesOnNameAndOnEmail() throws Exception {
        seedSearchFixtures();

        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A).param("q", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Alice Anderson")));

        // Matches the email even though the name has nothing in common with it.
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A).param("q", "test.org"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Bob Brown")));

        // A null email must not stop the name from matching.
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A).param("q", "NoEmail"))
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A).param("q", "zzz"))
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    void searchIsCaseInsensitive() throws Exception {
        seedSearchFixtures();

        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A).param("q", "ALICE"))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A).param("q", "aLiCe"))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void treatsLikeWildcardsAsLiteralText() throws Exception {
        seedSearchFixtures();

        // Unescaped, "%" and "_" would match every row instead of one.
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A).param("q", "%"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("50% Off Ltd")));
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A).param("q", "_"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Under_score Co")));
    }

    @Test
    void blankSearchIsNoFilter() throws Exception {
        seedSearchFixtures();

        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A).param("q", "   "))
                .andExpect(jsonPath("$.page.totalElements", is(5)));
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A))
                .andExpect(jsonPath("$.page.totalElements", is(5)));
    }

    @Test
    void searchFiltersBeforePagingAndSorting() throws Exception {
        seedSearchFixtures();

        // "deals" matches two rows; totalElements must reflect the match, not the table.
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A)
                        .param("q", "deals").param("size", "1").param("sort", "name,asc"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("50% Off Ltd")))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(2)));

        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A)
                        .param("q", "deals").param("size", "1").param("page", "1").param("sort", "name,asc"))
                .andExpect(jsonPath("$.content[0].name", is("Under_score Co")));
    }

    @Test
    void searchStaysWithinTheTenant() throws Exception {
        create(TENANT_A, "Alice Anderson", "alice@example.com");

        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_B).param("q", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void rejectsAnUnknownSortField() throws Exception {
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A)
                        .param("sort", "nonexistent,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("nonexistent")));
    }

    @Test
    void rejectsAMalformedSortDirection() throws Exception {
        // Spring Data reads an unparseable direction as a second property, which
        // would otherwise reach the repository and fail as a 500.
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A)
                        .param("sort", "name,sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("sideways")));
    }

    @Test
    void rejectsAMultiSortWhereOneFieldIsUnknown() throws Exception {
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A)
                        .param("sort", "name,asc").param("sort", "bogus,desc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsEverySortableField() throws Exception {
        for (String field : new String[]{"id", "name", "email", "createdAt"}) {
            mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A)
                            .param("sort", field + ",desc"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void capsAnOversizedPageRequest() throws Exception {
        mockMvc.perform(post("/customers").header(TENANT_HEADER, TENANT_A)
                .contentType(MediaType.APPLICATION_JSON).content(ALICE)).andExpect(status().isCreated());

        // spring.data.web.pageable.max-page-size is 100, so a huge size is clamped.
        mockMvc.perform(get("/customers").header(TENANT_HEADER, TENANT_A).param("size", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size", is(100)));
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
