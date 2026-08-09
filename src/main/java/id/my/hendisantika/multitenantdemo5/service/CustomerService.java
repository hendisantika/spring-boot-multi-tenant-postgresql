package id.my.hendisantika.multitenantdemo5.service;

import id.my.hendisantika.multitenantdemo5.dto.CustomerRequest;
import id.my.hendisantika.multitenantdemo5.entity.Customer;
import id.my.hendisantika.multitenantdemo5.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * Every method acts on the schema of the tenant on the current thread, so there
 * is no tenant argument anywhere: the X-TenantID header decides which schema is
 * read or written.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<Customer> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        return customerRepository.save(new Customer(request.name(), request.email()));
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> notFound(id));
        customer.setName(request.name());
        customer.setEmail(request.email());
        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw notFound(id);
        }
        customerRepository.deleteById(id);
    }

    private EntityNotFoundException notFound(Long id) {
        return new EntityNotFoundException("Customer " + id + " not found");
    }
}
