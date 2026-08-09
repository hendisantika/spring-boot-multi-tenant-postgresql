package id.my.hendisantika.multitenantdemo5.config;

import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by IntelliJ IDEA.
 * Project : multitenant-demo5
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 11/06/25
 * Time: 08.19
 * To change this template use File | Settings | File Templates.
 */
@Configuration
public class HibernateConfig {

    @Bean
    public CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver() {
        return new CurrentTenantIdentifierResolverImpl();
    }

    /**
     * Hands the multi-tenancy SPI implementations to the auto-configured
     * EntityManagerFactory. Declaring our own LocalContainerEntityManagerFactoryBean
     * instead would bypass Boot's Hibernate setup, losing the dialect, ddl-auto and
     * naming strategies configured in application.properties. Hibernate switches to
     * schema-per-tenant on its own once a MultiTenantConnectionProvider is present.
     */
    @Bean
    public HibernatePropertiesCustomizer multiTenancyCustomizer(
            MultiTenantConnectionProviderImpl multiTenantConnectionProvider,
            CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver) {

        return properties -> {
            properties.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
            properties.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);
        };
    }
}
