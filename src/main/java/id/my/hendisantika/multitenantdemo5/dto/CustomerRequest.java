package id.my.hendisantika.multitenantdemo5.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
 * <p>
 * Incoming payload. Kept separate from the entity so callers cannot set id or
 * createdAt.
 */
public record CustomerRequest(

        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @Email(message = "email must be a well-formed address")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email
) {
}
