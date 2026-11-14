package com.jobtracker.service;

import com.jobtracker.dto.StageReorderRequest;
import com.jobtracker.dto.StageRequest;
import com.jobtracker.dto.StageResponse;
import com.jobtracker.entity.ApplicationStage;
import com.jobtracker.repository.StageRepository;
import com.jobtracker.repository.UserRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StageService {
    private final StageRepository stages;
    private final UserRepository users;

    public StageService(StageRepository stages, UserRepository users) {
        this.stages = stages;
        this.users = users;
    }

    private com.jobtracker.entity.User user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }

    private StageResponse dto(ApplicationStage s) {
        return new StageResponse(
                s.getId(), s.getName(), s.getPosition(), s.getColor(), s.isFinal(), s.isActive());
    }

    @Transactional(readOnly = true)
    public List<StageResponse> list(String email) {
        return stages.findByUserIdAndIsActiveTrueOrderByPosition(user(email).getId()).stream()
                .map(this::dto)
                .toList();
    }

    @Transactional(readOnly = true)
    public StageResponse get(Long id, String email) {
        return dto(owned(id, email));
    }

    @Transactional
    public StageResponse create(StageRequest r, String email) {
        var u = user(email);
        if (stages.existsByUserIdAndNameIgnoreCase(u.getId(), r.name()))
            throw new IllegalStateException("Já existe uma etapa com esse nome.");
        return dto(
                stages.save(
                        new ApplicationStage(u, r.name(), r.position(), r.color(), r.isFinal())));
    }

    @Transactional
    public StageResponse update(Long id, StageRequest r, String email) {
        var s = owned(id, email);
        if (stages.existsByUserIdAndNameIgnoreCaseAndIdNot(s.getUser().getId(), r.name(), id))
            throw new IllegalStateException("Já existe uma etapa com esse nome.");
        s.update(r.name(), r.position(), r.color(), r.isFinal());
        return dto(s);
    }

    @Transactional
    public void deactivate(Long id, String email) {
        owned(id, email).deactivate();
    }

    private ApplicationStage owned(Long id, String email) {
        var s =
                stages.findById(id)
                        .filter(ApplicationStage::isActive)
                        .orElseThrow(() -> new NoSuchElementException("Etapa não encontrada."));
        if (!s.getUser().getEmail().equalsIgnoreCase(email))
            throw new org.springframework.security.access.AccessDeniedException(
                    "Etapa pertence a outro usuário.");
        return s;
    }

    @Transactional
    public void reorder(StageReorderRequest r, String email) {
        var list = stages.findByUserIdAndIsActiveTrueOrderByPosition(user(email).getId());
        if (r.stageIds() == null
                || r.stageIds().size() != list.size()
                || !new HashSet<>(r.stageIds())
                        .equals(
                                list.stream()
                                        .map(ApplicationStage::getId)
                                        .collect(java.util.stream.Collectors.toSet())))
            throw new IllegalArgumentException(
                    "Informe todos os IDs das etapas ativas, sem duplicidade.");
        for (int i = 0; i < r.stageIds().size(); i++) {
            final int pos = i;
            stages.findById(r.stageIds().get(i))
                    .orElseThrow()
                    .update(
                            stages.findById(r.stageIds().get(i)).orElseThrow().getName(),
                            pos,
                            stages.findById(r.stageIds().get(i)).orElseThrow().getColor(),
                            stages.findById(r.stageIds().get(i)).orElseThrow().isFinal());
        }
    }
}
