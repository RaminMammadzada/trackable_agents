package com.trackableagents.controlplane.web;

import com.trackableagents.controlplane.api.LessonDecisionRequest;
import com.trackableagents.controlplane.api.LessonProposalRequest;
import com.trackableagents.controlplane.api.LessonResponse;
import com.trackableagents.controlplane.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {

    private final LearningService learningService;

    public LessonController(LearningService learningService) {
        this.learningService = learningService;
    }

    @PostMapping("/proposals")
    public LessonResponse create(@Valid @RequestBody LessonProposalRequest request) {
        return learningService.createProposal(request);
    }

    @PostMapping("/{lessonId}/approve")
    public LessonResponse approve(@PathVariable String lessonId, @RequestBody(required = false) LessonDecisionRequest request) {
        return learningService.approve(lessonId, request);
    }

    @PostMapping("/{lessonId}/reject")
    public LessonResponse reject(@PathVariable String lessonId, @RequestBody(required = false) LessonDecisionRequest request) {
        return learningService.reject(lessonId, request);
    }
}

