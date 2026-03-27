package com.rjs.fsm.customer;

import com.rjs.fsm.customer.dto.CustomerResponse;
import com.rjs.fsm.customer.dto.SaveCustomerRequest;
import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository repo;

    public CustomerService(CustomerRepository repo) {
        this.repo = repo;
    }

    public List<CustomerResponse> listAll(String search) {
        UUID tenantId = TenantContext.require();
        List<Customer> result = (search != null && !search.isBlank())
                ? repo.findByTenantIdAndNameContainingIgnoreCaseOrderByNameAsc(tenantId, search.trim())
                : repo.findByTenantIdOrderByNameAsc(tenantId);
        return result.stream().map(CustomerResponse::from).toList();
    }

    @Transactional
    public CustomerResponse create(SaveCustomerRequest req) {
        UUID tenantId = TenantContext.require();
        Customer c = new Customer();
        c.setTenantId(tenantId);
        applyRequest(c, req);
        return CustomerResponse.from(repo.save(c));
    }

    @Transactional
    public CustomerResponse update(UUID id, SaveCustomerRequest req) {
        UUID tenantId = TenantContext.require();
        Customer c = repo.findById(id)
                .filter(x -> x.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BadRequestException("Customer tidak ditemukan"));
        applyRequest(c, req);
        return CustomerResponse.from(repo.save(c));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.require();
        Customer c = repo.findById(id)
                .filter(x -> x.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BadRequestException("Customer tidak ditemukan"));
        repo.delete(c);
    }

    private void applyRequest(Customer c, SaveCustomerRequest req) {
        c.setName(req.getName().trim());
        c.setPhoneE164(req.getPhoneE164() != null ? req.getPhoneE164().trim() : null);
        c.setAddress(req.getAddress() != null ? req.getAddress().trim() : null);
        c.setMachineType(req.getMachineType() != null ? req.getMachineType().trim() : null);
        c.setMachineNumber(req.getMachineNumber() != null ? req.getMachineNumber().trim() : null);
        c.setNotes(req.getNotes() != null ? req.getNotes().trim() : null);
    }
}
