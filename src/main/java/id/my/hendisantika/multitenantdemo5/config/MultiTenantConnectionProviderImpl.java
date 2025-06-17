package id.my.hendisantika.multitenantdemo5.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
public class MultiTenantConnectionProviderImpl extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    private final DataSource dataSource;

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSource;
    }

    @Override
    protected DataSource selectDataSource(Object tenantIdentifier) {
        return dataSource;
    }

    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        String tenantId = tenantIdentifier != null ? tenantIdentifier.toString() : "public";
        log.info("Acquiring connection for tenant {}", tenantId);
        Connection connection = getAnyConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute(String.format("SET search_path TO %s;", tenantId));
        }
        return connection;
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO public;");
        }
        releaseAnyConnection(connection);
    }
}
