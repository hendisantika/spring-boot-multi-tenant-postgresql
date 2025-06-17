package id.my.hendisantika.multitenantdemo5.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Created by IntelliJ IDEA.
 * Project : multitenant-demo5
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 11/06/25
 * Time: 08.21
 * To change this template use File | Settings | File Templates.
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final JdbcTemplate jdbcTemplate;

    public void createTenant(String tenantName) {
        // Validate tenant name: allow only alphanumeric characters and underscores.
        if (!tenantName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid tenant name.");
        }
        String sql = "CREATE SCHEMA IF NOT EXISTS " + tenantName;
        jdbcTemplate.execute(sql);
    }
}
