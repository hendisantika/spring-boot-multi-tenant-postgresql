package id.my.hendisantika.multitenantdemo5.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

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
@Component
@RequiredArgsConstructor
public class MultiTenantConnectionProviderImpl extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> {

    private static final String DEFAULT_TENANT = "public";
    private static final Pattern VALID_TENANT = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final DataSource dataSource;

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSource;
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        return dataSource;
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        String tenantId = tenantIdentifier != null ? tenantIdentifier : DEFAULT_TENANT;
        // The tenant id ends up in a SET search_path statement, which cannot be
        // parameterised, so it must be validated as a plain SQL identifier first.
        if (!VALID_TENANT.matcher(tenantId).matches()) {
            throw new SQLException("Invalid tenant identifier: " + tenantId);
        }
        log.debug("Acquiring connection for tenant {}", tenantId);
        Connection connection = getAnyConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + tenantId);
        } catch (SQLException e) {
            releaseAnyConnection(connection);
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + DEFAULT_TENANT);
        } finally {
            releaseAnyConnection(connection);
        }
    }
}
