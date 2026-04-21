package com.trackableagents.controlplane.config;

import com.trackableagents.controlplane.learning.FailureRecordRepository;
import com.trackableagents.controlplane.learning.LessonProposalRepository;
import com.trackableagents.controlplane.ledger.AgentEventRepository;
import com.trackableagents.controlplane.projection.RunProjectionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TrackableAgentsMetricsBinder implements MeterBinder {

    private final RunProjectionRepository runProjectionRepository;
    private final AgentEventRepository agentEventRepository;
    private final FailureRecordRepository failureRecordRepository;
    private final LessonProposalRepository lessonProposalRepository;
    private final List<MetricSupplier> metricSuppliers = new ArrayList<>();

    public TrackableAgentsMetricsBinder(
        RunProjectionRepository runProjectionRepository,
        AgentEventRepository agentEventRepository,
        FailureRecordRepository failureRecordRepository,
        LessonProposalRepository lessonProposalRepository
    ) {
        this.runProjectionRepository = runProjectionRepository;
        this.agentEventRepository = agentEventRepository;
        this.failureRecordRepository = failureRecordRepository;
        this.lessonProposalRepository = lessonProposalRepository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registerGauge(registry, "trackable_agents_runs_current", "Current total tracked runs", runProjectionRepository::count);
        registerGauge(registry, "trackable_agents_events_current", "Current total ingested events", agentEventRepository::count);
        registerGauge(registry, "trackable_agents_failures_current", "Current total recorded failures", failureRecordRepository::count);
        registerGauge(registry, "trackable_agents_lessons_current", "Current total lesson proposals", lessonProposalRepository::count);

        registerGauge(registry, "trackable_agents_runs_by_status", "Runs by status", "status", "active",
            () -> runProjectionRepository.countByStatus("active"));
        registerGauge(registry, "trackable_agents_runs_by_status", "Runs by status", "status", "completed",
            () -> runProjectionRepository.countByStatus("completed"));
        registerGauge(registry, "trackable_agents_runs_by_status", "Runs by status", "status", "attention_required",
            () -> runProjectionRepository.countByStatus("attention_required"));

        registerGauge(registry, "trackable_agents_runs_by_source", "Runs by source backend", "source", "codex",
            () -> runProjectionRepository.countBySource("codex"));
        registerGauge(registry, "trackable_agents_runs_by_source", "Runs by source backend", "source", "claude",
            () -> runProjectionRepository.countBySource("claude"));
        registerGauge(registry, "trackable_agents_runs_by_source", "Runs by source backend", "source", "copilot",
            () -> runProjectionRepository.countBySource("copilot"));
        registerGauge(registry, "trackable_agents_runs_by_source", "Runs by source backend", "source", "github",
            () -> runProjectionRepository.countBySource("github"));

        registerGauge(registry, "trackable_agents_events_by_source", "Events by source backend", "source", "codex",
            () -> agentEventRepository.countBySource("codex"));
        registerGauge(registry, "trackable_agents_events_by_source", "Events by source backend", "source", "claude",
            () -> agentEventRepository.countBySource("claude"));
        registerGauge(registry, "trackable_agents_events_by_source", "Events by source backend", "source", "copilot",
            () -> agentEventRepository.countBySource("copilot"));
        registerGauge(registry, "trackable_agents_events_by_source", "Events by source backend", "source", "github",
            () -> agentEventRepository.countBySource("github"));

        registerGauge(registry, "trackable_agents_failures_by_status", "Failures by status", "status", "open",
            () -> failureRecordRepository.countByStatus("open"));
        registerGauge(registry, "trackable_agents_failures_by_status", "Failures by status", "status", "resolved",
            () -> failureRecordRepository.countByStatus("resolved"));

        registerGauge(registry, "trackable_agents_lessons_by_status", "Lessons by status", "status", "pending",
            () -> lessonProposalRepository.countByStatus("pending"));
        registerGauge(registry, "trackable_agents_lessons_by_status", "Lessons by status", "status", "approved",
            () -> lessonProposalRepository.countByStatus("approved"));
        registerGauge(registry, "trackable_agents_lessons_by_status", "Lessons by status", "status", "rejected",
            () -> lessonProposalRepository.countByStatus("rejected"));
    }

    private void registerGauge(MeterRegistry registry, String name, String description, MetricSupplier supplier) {
        metricSuppliers.add(supplier);
        Gauge.builder(name, supplier, MetricSupplier::get)
            .description(description)
            .register(registry);
    }

    private void registerGauge(
        MeterRegistry registry,
        String name,
        String description,
        String tagKey,
        String tagValue,
        MetricSupplier supplier
    ) {
        metricSuppliers.add(supplier);
        Gauge.builder(name, supplier, MetricSupplier::get)
            .description(description)
            .tag(tagKey, tagValue)
            .register(registry);
    }

    @FunctionalInterface
    private interface MetricSupplier {
        double get();
    }
}
