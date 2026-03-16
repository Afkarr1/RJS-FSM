package com.rjs.fsm.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tech")
public class TechController {

    @GetMapping("/ping")
    public String ping() {
        return "TECH OK";
    }
}
