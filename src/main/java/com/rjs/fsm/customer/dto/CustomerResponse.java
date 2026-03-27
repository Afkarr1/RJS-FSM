package com.rjs.fsm.customer.dto;

import com.rjs.fsm.customer.Customer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CustomerResponse {
    private UUID id;
    private String name;
    private String phoneE164;
    private String address;
    private String machineType;
    private String machineNumber;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getName(), c.getPhoneE164(), c.getAddress(),
                c.getMachineType(), c.getMachineNumber(), c.getNotes(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
