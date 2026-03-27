package com.rjs.fsm.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SaveCustomerRequest {

    @NotBlank(message = "Nama customer wajib diisi")
    @Size(max = 150, message = "Nama maksimal 150 karakter")
    private String name;

    @Size(max = 20, message = "Nomor telepon maksimal 20 karakter")
    private String phoneE164;

    private String address;

    @Size(max = 100, message = "Tipe mesin maksimal 100 karakter")
    private String machineType;

    @Size(max = 100, message = "Nomor mesin maksimal 100 karakter")
    private String machineNumber;

    private String notes;
}
