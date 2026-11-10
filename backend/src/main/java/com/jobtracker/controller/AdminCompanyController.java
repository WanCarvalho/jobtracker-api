package com.jobtracker.controller;

import com.jobtracker.dto.CompanyResponse;
import com.jobtracker.dto.CompanyReviewRequest;
import com.jobtracker.service.CompanyService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/companies") @PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminCompanyController {
 private final CompanyService service;
 public AdminCompanyController(CompanyService service){this.service=service;}
 @GetMapping List<CompanyResponse> all(){return service.adminList();}
 @GetMapping("/pending") List<CompanyResponse> pending(){return service.pending();}
 @PatchMapping("/{id}/approve") CompanyResponse approve(@PathVariable Long id,Authentication a){return service.approve(id,a.getName());}
 @PatchMapping("/{id}/reject") CompanyResponse reject(@PathVariable Long id,@Valid @RequestBody CompanyReviewRequest r,Authentication a){return service.reject(id,a.getName(),r.reason());}
}
