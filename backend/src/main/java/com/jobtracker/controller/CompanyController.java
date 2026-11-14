package com.jobtracker.controller;

import com.jobtracker.dto.CompanyRequest;
import com.jobtracker.dto.CompanyResponse;
import com.jobtracker.service.CompanyService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest r, Authentication a) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r, a.getName()));
    }

    @GetMapping
    List<CompanyResponse> list(Authentication a) {
        return service.list(a.getName());
    }

    @GetMapping("/{id}")
    CompanyResponse get(@PathVariable Long id, Authentication a) {
        return service.get(id, a.getName());
    }

    @PutMapping("/{id}")
    CompanyResponse update(
            @PathVariable Long id, @Valid @RequestBody CompanyRequest r, Authentication a) {
        return service.update(id, r, a.getName());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id, Authentication a) {
        service.delete(id, a.getName());
        return ResponseEntity.noContent().build();
    }
}
