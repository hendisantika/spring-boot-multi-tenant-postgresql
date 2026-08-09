package id.my.hendisantika.multitenantdemo5.controller;

import id.my.hendisantika.multitenantdemo5.dto.CustomerRequest;
import id.my.hendisantika.multitenantdemo5.dto.CustomerResponse;
import id.my.hendisantika.multitenantdemo5.service.CustomerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * Sorting is restricted to real, safe-to-expose columns. Anything else would
     * reach Spring Data as a property reference and blow up as a 500 rather than
     * telling the caller what they got wrong.
     */
    private static final List<String> SORTABLE_FIELDS = List.of("id", "name", "email", "createdAt");

    private final CustomerService customerService;

    /**
     * Paged so a tenant with a large table cannot be dumped in one response.
     * Accepts the usual page/size/sort parameters, e.g. ?page=1&size=50&sort=name,asc.
     * spring.data.web.pageable.max-page-size caps how large size may go.
     * <p>
     * An optional {@code q} narrows the page to customers whose name or email
     * contains it, case-insensitively. It filters before paging, so totalElements
     * reflects the matches rather than the whole table.
     */
    @GetMapping
    public PagedModel<CustomerResponse> list(@RequestParam(name = "q", required = false) String q,
                                             @PageableDefault(sort = "id") Pageable pageable) {
        validateSort(pageable.getSort());
        return new PagedModel<>(customerService.findAll(q, pageable).map(CustomerResponse::from));
    }

    private static void validateSort(Sort sort) {
        List<String> unknown = sort.stream()
                .map(Sort.Order::getProperty)
                .filter(property -> !SORTABLE_FIELDS.contains(property))
                .toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot sort by " + unknown + ". Sortable fields are " + SORTABLE_FIELDS
                            + ". Note that a malformed direction is read as another field,"
                            + " so use sort=field,asc or sort=field,desc.");
        }
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

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException ex) {
        return ex.getMessage();
    }

    /**
     * Backstop in case a property slips past the allow list above, so an unknown
     * sort field can never surface as a 500.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleUnknownProperty(PropertyReferenceException ex) {
        return ex.getMessage();
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
