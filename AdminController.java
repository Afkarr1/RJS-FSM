package com.rjs.fsm.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.rjs.fsm.job.dto.JobStatusHistoryResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/ping")
    public String ping() {
        return "ADMIN OK";
    }
}