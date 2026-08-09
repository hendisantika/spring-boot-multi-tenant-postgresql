package id.my.hendisantika.multitenantdemo5.controller;

import id.my.hendisantika.multitenantdemo5.dto.CustomerRequest;
import id.my.hendisantika.multitenantdemo5.dto.CustomerResponse;
import id.my.hendisantika.multitenantdemo5.service.CustomerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

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
 * CRUD over the customers table of a single tenant. The tenant comes from the
 * X-TenantID header, which AppTenantContext turns into the schema every query
 * below runs against; without the header the request lands on `public`.
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public List<CustomerResponse> list() {
        return customerService.findAll().stream().map(CustomerResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return CustomerResponse.from(customerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        CustomerResponse created = CustomerResponse.from(customerService.create(request));
        return ResponseEntity
                .created(uriBuilder.path("/customers/{id}").build(created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException ex) {
        return ex.getMessage();
    }

    /**
     * customers carries a unique index on email, scoped to the tenant's schema.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDuplicate(DataIntegrityViolationException ex) {
        return "Customer violates a uniqueness constraint (email must be unique within the tenant).";
    }
}
