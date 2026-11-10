package com.jobtracker.controller;

import com.jobtracker.service.DashboardService;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    Map<String, Long> summary(Authentication auth) {
        return service.summary(auth.getName());
    }

    @GetMapping("/status-distribution")
    List<Map<String, Object>> distribution(Authentication auth) {
        return service.distribution(auth.getName());
    }
}
