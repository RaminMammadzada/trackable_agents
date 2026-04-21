package com.trackableagents.controlplane.web;

import com.trackableagents.controlplane.api.AdminActionResponse;
import com.trackableagents.controlplane.service.AdminRuntimeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminRuntimeService adminRuntimeService;

    public AdminController(AdminRuntimeService adminRuntimeService) {
        this.adminRuntimeService = adminRuntimeService;
    }

    @PostMapping("/demo/seed")
    public AdminActionResponse seedDemoData() {
        return adminRuntimeService.seedDemoData();
    }

    @PostMapping("/runtime/reset")
    public AdminActionResponse resetRuntimeData() {
        return adminRuntimeService.resetRuntimeData();
    }
}
