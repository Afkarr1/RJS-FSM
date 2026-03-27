package com.rjs.fsm.customer;

import com.rjs.fsm.customer.dto.CustomerResponse;
import com.rjs.fsm.customer.dto.SaveCustomerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final CustomerService service;

    public AdminCustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerResponse> list(@RequestParam(required = false) String search) {
        return service.listAll(search);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody SaveCustomerRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody SaveCustomerRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
