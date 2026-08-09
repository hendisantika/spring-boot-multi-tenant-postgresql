package id.my.hendisantika.multitenantdemo5.repository;

import id.my.hendisantika.multitenantdemo5.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created by IntelliJ IDEA.
 * Project : multitenant-demo5
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 09/08/26
 * Time: 08.44
 * To change this template use File | Settings | File Templates.
 * <p>
 * Every query runs against the schema of the tenant on the current thread, so
 * the same repository serves all tenants without any tenant column or filter.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);
}
