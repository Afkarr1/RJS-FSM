package com.rjs.fsm.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public Map<String, Object> me(Authentication auth) {
        return Map.of(
                "username", auth.getName(),
                "authorities", auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList()
        );
    }
}
