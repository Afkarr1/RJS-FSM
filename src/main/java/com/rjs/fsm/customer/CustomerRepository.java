package com.rjs.fsm.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByTenantIdOrderByNameAsc(UUID tenantId);

    List<Customer> findByTenantIdAndNameContainingIgnoreCaseOrderByNameAsc(UUID tenantId, String name);
}
