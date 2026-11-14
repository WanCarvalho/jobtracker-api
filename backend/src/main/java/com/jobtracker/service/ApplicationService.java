package com.jobtracker.service;

import com.jobtracker.dto.ApplicationHistoryResponse;
import com.jobtracker.dto.ApplicationNoteRequest;
import com.jobtracker.dto.ApplicationNoteResponse;
import com.jobtracker.dto.ApplicationRequest;
import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.ApplicationStageChange;
import com.jobtracker.entity.ApplicationHistory;
import com.jobtracker.entity.ApplicationNote;
import com.jobtracker.entity.ApplicationStage;
import com.jobtracker.entity.Company;
import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.User;
import com.jobtracker.enums.CompanyApprovalStatus;
import com.jobtracker.repository.ApplicationRepository;
import com.jobtracker.repository.CompanyRepository;
import com.jobtracker.repository.HistoryRepository;
import com.jobtracker.repository.NoteRepository;
import com.jobtracker.repository.StageRepository;
import com.jobtracker.repository.UserRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {
    private final ApplicationRepository apps;
    private final CompanyRepository companies;
    private final StageRepository stages;
    private final HistoryRepository histories;
    private final NoteRepository notes;
    private final UserRepository users;

    public ApplicationService(
            ApplicationRepository a,
            CompanyRepository c,
            StageRepository s,
            HistoryRepository h,
            NoteRepository n,
            UserRepository u) {
        apps = a;
        companies = c;
        stages = s;
        histories = h;
        notes = n;
        users = u;
    }

    private User user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }

    private JobApplication owned(Long id, String email) {
        var a =
                apps.findById(id)
                        .filter(record -> !record.isDeleted())
                        .orElseThrow(
                                () -> new NoSuchElementException("Candidatura não encontrada."));
        if (!a.getUser().getEmail().equalsIgnoreCase(email))
            throw new org.springframework.security.access.AccessDeniedException(
                    "Candidatura pertence a outro usuário.");
        return a;
    }

    private Company company(Long id, User u) {
        var c =
                companies
                        .findById(id)
                        .filter(record -> !record.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("Empresa não encontrada."));
        if (c.getApprovalStatus() != CompanyApprovalStatus.APPROVED
                && !c.getCreatedBy().getId().equals(u.getId()))
            throw new IllegalArgumentException(
                    "Empresa ainda não está disponível para este usuário.");
        return c;
    }

    private ApplicationStage stage(Long id, User u) {
        var s =
                stages.findById(id)
                        .orElseThrow(() -> new NoSuchElementException("Etapa não encontrada."));
        if (!s.getUser().getId().equals(u.getId()) || !s.isActive())
            throw new IllegalArgumentException("Etapa inválida ou indisponível.");
        return s;
    }

    private ApplicationResponse dto(JobApplication a) {
        return new ApplicationResponse(
                a.getId(),
                a.getCompany().getId(),
                a.getCompany().getName(),
                a.getStage().getId(),
                a.getStage().getName(),
                a.getStage().getColor(),
                a.getStage().isFinal(),
                a.getJobTitle(),
                a.getSalary(),
                a.getJobUrl(),
                a.getDescription(),
                a.getAppliedAt());
    }

    @Transactional
    public ApplicationResponse create(ApplicationRequest r, String email) {
        User u = user(email);
        var c = company(r.companyId(), u);
        var s = stage(r.stageId(), u);
        var a =
                apps.save(
                        new JobApplication(
                                u,
                                c,
                                s,
                                r.jobTitle(),
                                r.salary(),
                                r.jobUrl(),
                                r.description(),
                                r.appliedAt()));
        histories.save(new ApplicationHistory(a, null, s, u, "Candidatura registrada."));
        return dto(a);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(String email) {
        return apps.findByUserIdAndDeletedAtIsNullOrderByAppliedAtDesc(user(email).getId()).stream()
                .map(this::dto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(Long id, String email) {
        return dto(owned(id, email));
    }

    @Transactional
    public ApplicationResponse update(Long id, ApplicationRequest r, String email) {
        User u = user(email);
        var a = owned(id, email);
        var old = a.getStage();
        var next = a.getStage().getId().equals(r.stageId()) ? a.getStage() : stage(r.stageId(), u);
        var c =
                a.getCompany().getId().equals(r.companyId())
                        ? a.getCompany()
                        : company(r.companyId(), u);
        a.update(c, next, r.jobTitle(), r.salary(), r.jobUrl(), r.description(), r.appliedAt());
        if (!old.getId().equals(next.getId()))
            histories.save(
                    new ApplicationHistory(
                            a, old, next, u, "Etapa alterada durante a atualização."));
        return dto(a);
    }

    @Transactional
    public ApplicationResponse changeStage(Long id, ApplicationStageChange r, String email) {
        User u = user(email);
        var a = owned(id, email);
        var next = stage(r.stageId(), u);
        var old = a.getStage();
        if (old.getId().equals(next.getId()))
            throw new IllegalStateException("A candidatura já está nesta etapa.");
        a.update(
                a.getCompany(),
                next,
                a.getJobTitle(),
                a.getSalary(),
                a.getJobUrl(),
                a.getDescription(),
                a.getAppliedAt());
        histories.save(new ApplicationHistory(a, old, next, u, r.notes()));
        return dto(a);
    }

    @Transactional
    public void delete(Long id, String email) {
        owned(id, email).softDelete();
    }

    @Transactional(readOnly = true)
    public List<ApplicationHistoryResponse> history(Long id, String email) {
        owned(id, email);
        return histories.findByApplicationIdOrderByChangedAtDesc(id).stream()
                .map(
                        h ->
                                new ApplicationHistoryResponse(
                                        h.getId(),
                                        h.getPreviousStage() == null
                                                ? null
                                                : h.getPreviousStage().getId(),
                                        h.getPreviousStage() == null
                                                ? null
                                                : h.getPreviousStage().getName(),
                                        h.getNewStage().getId(),
                                        h.getNewStage().getName(),
                                        h.getChangedBy().getName(),
                                        h.getNotes(),
                                        h.getChangedAt()))
                .toList();
    }

    @Transactional
    public ApplicationNoteResponse addNote(Long id, ApplicationNoteRequest r, String email) {
        var a = owned(id, email);
        var u = user(email);
        var n = notes.save(new ApplicationNote(a, u, r.content()));
        return new ApplicationNoteResponse(
                n.getId(), n.getContent(), u.getName(), n.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ApplicationNoteResponse> listNotes(Long id, String email) {
        owned(id, email);
        return notes.findByApplicationIdOrderByCreatedAtDesc(id).stream()
                .map(
                        n ->
                                new ApplicationNoteResponse(
                                        n.getId(),
                                        n.getContent(),
                                        n.getUser().getName(),
                                        n.getCreatedAt()))
                .toList();
    }
}
