package id.my.hendisantika.multitenantdemo5.controller;

import id.my.hendisantika.multitenantdemo5.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/create")
    public String createTenant(@RequestParam("tenantName") String tenantName) {
        tenantService.createTenant(tenantName);
        return "Tenant " + tenantName + " created successfully";
    }
}
