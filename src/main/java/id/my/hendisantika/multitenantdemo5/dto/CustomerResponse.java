package id.my.hendisantika.multitenantdemo5.dto;

import id.my.hendisantika.multitenantdemo5.entity.Customer;

import java.time.OffsetDateTime;

/**
 * Created by IntelliJ IDEA.
 * Project : multitenant-demo5
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 09/08/26
 * Time: 08.58
 * To change this template use File | Settings | File Templates.
 */
public record CustomerResponse(
        Long id,
        String name,
        String email,
        OffsetDateTime createdAt
) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCreatedAt()
        );
    }
}
