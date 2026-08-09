package id.my.hendisantika.multitenantdemo5.repository;

import id.my.hendisantika.multitenantdemo5.config.AppTenantContext;
import id.my.hendisantika.multitenantdemo5.entity.Customer;
import id.my.hendisantika.multitenantdemo5.service.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the same repository reads and writes a different schema depending
 * on the tenant on the current thread. Deliberately not @Transactional: a single
 * transaction would pin one Hibernate session, and therefore one tenant, for the
 * whole test.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = "spring.docker.compose.enabled=false")
class CustomerRepositoryTest {

    private static final String TENANT_A = "tenant_a";
    private static final String TENANT_B = "tenant_b";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18.4-alpine3.24");

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TenantService tenantService;

    @BeforeEach
    void createTenants() {
        // Also migrates each new schema, so `customers` exists in both.
        tenantService.createTenant(TENANT_A);
        tenantService.createTenant(TENANT_B);
        for (String tenant : new String[]{TENANT_A, TENANT_B}) {
            AppTenantContext.setCurrentTenant(tenant);
            customerRepository.deleteAll();
        }
        AppTenantContext.clear();
    }

    @AfterEach
    void clearTenant() {
        AppTenantContext.clear();
    }

    @Test
    void savesIntoTheSchemaOfTheCurrentTenant() {
        AppTenantContext.setCurrentTenant(TENANT_A);
        Customer saved = customerRepository.save(new Customer("Alice", "alice@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(customerRepository.findByEmail("alice@example.com")).isPresent();
    }

    @Test
    void doesNotLeakRowsAcrossTenants() {
        AppTenantContext.setCurrentTenant(TENANT_A);
        customerRepository.save(new Customer("Alice", "alice@example.com"));

        AppTenantContext.setCurrentTenant(TENANT_B);
        assertThat(customerRepository.count()).isZero();
        assertThat(customerRepository.existsByEmail("alice@example.com")).isFalse();

        // The same email is free in another tenant's schema.
        customerRepository.save(new Customer("Bob", "alice@example.com"));
        assertThat(customerRepository.count()).isEqualTo(1);

        AppTenantContext.setCurrentTenant(TENANT_A);
        assertThat(customerRepository.count()).isEqualTo(1);
        assertThat(customerRepository.findByEmail("alice@example.com"))
                .get()
                .extracting(Customer::getName)
                .isEqualTo("Alice");
    }
}
