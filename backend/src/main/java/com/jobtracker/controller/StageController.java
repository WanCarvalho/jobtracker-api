package com.jobtracker.controller;

import com.jobtracker.dto.StageReorderRequest;
import com.jobtracker.dto.StageRequest;
import com.jobtracker.dto.StageResponse;
import com.jobtracker.service.StageService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stages")
public class StageController {
    private final StageService service;

    public StageController(StageService service) {
        this.service = service;
    }

    @GetMapping
    List<StageResponse> list(Authentication a) {
        return service.list(a.getName());
    }

    @GetMapping("/{id}")
    StageResponse get(@PathVariable Long id, Authentication a) {
        return service.get(id, a.getName());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id, Authentication a) {
        service.deactivate(id, a.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    ResponseEntity<StageResponse> create(@Valid @RequestBody StageRequest r, Authentication a) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r, a.getName()));
    }

    @PutMapping("/{id}")
    StageResponse update(@PathVariable Long id, @Valid @RequestBody StageRequest r, Authentication a) {
        return service.update(id, r, a.getName());
    }

    @PatchMapping("/reorder")
    ResponseEntity<Void> reorder(@RequestBody StageReorderRequest r, Authentication a) {
        service.reorder(r, a.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    ResponseEntity<Void> deactivate(@PathVariable Long id, Authentication a) {
        service.deactivate(id, a.getName());
        return ResponseEntity.noContent().build();
    }
}
