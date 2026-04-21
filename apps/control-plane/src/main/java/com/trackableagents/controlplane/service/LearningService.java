package com.trackableagents.controlplane.service;

import com.trackableagents.controlplane.api.LessonDecisionRequest;
import com.trackableagents.controlplane.api.LessonProposalRequest;
import com.trackableagents.controlplane.api.LessonResponse;
import com.trackableagents.controlplane.learning.LessonProposalEntity;
import com.trackableagents.controlplane.learning.LessonProposalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class LearningService {

    private final LessonProposalRepository lessonProposalRepository;
    private final JsonMapper jsonMapper;

    public LearningService(LessonProposalRepository lessonProposalRepository, JsonMapper jsonMapper) {
        this.lessonProposalRepository = lessonProposalRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public LessonResponse createProposal(LessonProposalRequest request) {
        LessonProposalEntity entity = new LessonProposalEntity();
        entity.setLessonId(UUID.randomUUID().toString());
        entity.setRunId(request.runId());
        entity.setFailureId(request.failureId());
        entity.setTitle(request.title());
        entity.setSummary(request.summary());
        entity.setDetailsJson(jsonMapper.write(request.details()));
        entity.setStatus("pending");
        entity.setCreatedAt(Instant.now());
        lessonProposalRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public LessonResponse approve(String lessonId, LessonDecisionRequest request) {
        LessonProposalEntity entity = findLesson(lessonId);
        entity.setStatus("approved");
        entity.setDecisionNote(request == null ? null : request.note());
        entity.setDecidedAt(Instant.now());
        return toResponse(lessonProposalRepository.save(entity));
    }

    @Transactional
    public LessonResponse reject(String lessonId, LessonDecisionRequest request) {
        LessonProposalEntity entity = findLesson(lessonId);
        entity.setStatus("rejected");
        entity.setDecisionNote(request == null ? null : request.note());
        entity.setDecidedAt(Instant.now());
        return toResponse(lessonProposalRepository.save(entity));
    }

    private LessonProposalEntity findLesson(String lessonId) {
        return lessonProposalRepository.findById(lessonId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown lessonId: " + lessonId));
    }

    private LessonResponse toResponse(LessonProposalEntity lesson) {
        return new LessonResponse(
            lesson.getLessonId(),
            lesson.getRunId(),
            lesson.getFailureId(),
            lesson.getTitle(),
            lesson.getSummary(),
            jsonMapper.read(lesson.getDetailsJson()),
            lesson.getStatus(),
            lesson.getDecisionNote(),
            lesson.getCreatedAt(),
            lesson.getDecidedAt()
        );
    }
}

