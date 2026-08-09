package id.my.hendisantika.multitenantdemo5.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

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
 * Deliberately declares no schema: the schema is whatever the current tenant
 * resolves to, applied as a search_path by MultiTenantConnectionProviderImpl.
 * The table itself is created per tenant by Flyway.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Email
    @Size(max = 255)
    @Column(name = "email")
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
