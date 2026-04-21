package com.trackableagents.controlplane.web;

import com.trackableagents.controlplane.api.DashboardSummaryResponse;
import com.trackableagents.controlplane.api.EventResponse;
import com.trackableagents.controlplane.api.FailureResponse;
import com.trackableagents.controlplane.api.LessonResponse;
import com.trackableagents.controlplane.api.RunDetailResponse;
import com.trackableagents.controlplane.api.RunSummaryResponse;
import com.trackableagents.controlplane.service.RunQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RunQueryController {

    private final RunQueryService runQueryService;

    public RunQueryController(RunQueryService runQueryService) {
        this.runQueryService = runQueryService;
    }

    @GetMapping("/runs")
    public List<RunSummaryResponse> runs() {
        return runQueryService.listRuns();
    }

    @GetMapping("/runs/{runId}")
    public RunDetailResponse run(@PathVariable String runId) {
        return runQueryService.getRun(runId);
    }

    @GetMapping("/runs/{runId}/events")
    public List<EventResponse> runEvents(@PathVariable String runId) {
        return runQueryService.getRunEvents(runId);
    }

    @GetMapping("/failures")
    public List<FailureResponse> failures() {
        return runQueryService.listFailures();
    }

    @GetMapping("/lessons")
    public List<LessonResponse> lessons() {
        return runQueryService.listLessons();
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse dashboardSummary() {
        return runQueryService.dashboardSummary();
    }
}
