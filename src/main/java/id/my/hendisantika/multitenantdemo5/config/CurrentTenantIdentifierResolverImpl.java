package id.my.hendisantika.multitenantdemo5.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import java.util.Objects;

/**
 * Created by IntelliJ IDEA.
 * Project : multitenant-demo5
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 11/06/25
 * Time: 08.18
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = AppTenantContext.getCurrentTenant();
        log.info("Resolving tenant identifier: {}", tenant);
        return Objects.requireNonNullElse(tenant, "public");
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
