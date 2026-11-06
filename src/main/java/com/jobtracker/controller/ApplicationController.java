package com.jobtracker.controller;

import com.jobtracker.dto.ApplicationHistoryResponse;
import com.jobtracker.dto.ApplicationNoteRequest;
import com.jobtracker.dto.ApplicationNoteResponse;
import com.jobtracker.dto.ApplicationRequest;
import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.ApplicationStageChange;
import com.jobtracker.service.ApplicationService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/applications")
public class ApplicationController {
 private final ApplicationService service;
 public ApplicationController(ApplicationService service){this.service=service;}
 @PostMapping ResponseEntity<ApplicationResponse> create(@Valid @RequestBody ApplicationRequest r,Authentication a){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r,a.getName()));}
 @GetMapping List<ApplicationResponse> list(Authentication a){return service.list(a.getName());}
 @GetMapping("/{id}") ApplicationResponse get(@PathVariable Long id,Authentication a){return service.get(id,a.getName());}
 @PutMapping("/{id}") ApplicationResponse update(@PathVariable Long id,@Valid @RequestBody ApplicationRequest r,Authentication a){return service.update(id,r,a.getName());}
 @DeleteMapping("/{id}") ResponseEntity<Void> delete(@PathVariable Long id,Authentication a){service.delete(id,a.getName());return ResponseEntity.noContent().build();}
 @PatchMapping("/{id}/stage") ApplicationResponse changeStage(@PathVariable Long id,@Valid @RequestBody ApplicationStageChange r,Authentication a){return service.changeStage(id,r,a.getName());}
 @GetMapping("/{id}/history") List<ApplicationHistoryResponse> history(@PathVariable Long id,Authentication a){return service.history(id,a.getName());}
 @GetMapping("/{id}/notes") List<ApplicationNoteResponse> notes(@PathVariable Long id,Authentication a){return service.listNotes(id,a.getName());}
 @PostMapping("/{id}/notes") ResponseEntity<ApplicationNoteResponse> addNote(@PathVariable Long id,@Valid @RequestBody ApplicationNoteRequest r,Authentication a){return ResponseEntity.status(HttpStatus.CREATED).body(service.addNote(id,r,a.getName()));}
}
