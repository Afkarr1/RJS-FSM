package com.rjs.fsm.job.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CloseJobRequest {

    @Size(max = 500, message = "Spare parts maksimal 500 karakter")
    private String spareParts;

    @Size(max = 1000, message = "Catatan penutupan maksimal 1000 karakter")
    private String closingNote;
}
