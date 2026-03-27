package com.rjs.fsm.job;

import com.rjs.fsm.job.dto.*;
import com.rjs.fsm.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/jobs")
public class AdminJobController {

    private final JobService jobService;
    private final CurrentUserProvider currentUser;

    public AdminJobController(JobService jobService, CurrentUserProvider currentUser) {
        this.jobService = jobService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest req) {
        UUID adminId = currentUser.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(req, adminId));
    }

    @GetMapping
    public List<JobResponse> listJobs(@RequestParam(required = false) JobStatus status) {
        if (status != null) return jobService.listJobsByStatus(status);
        return jobService.listAllJobs();
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable UUID id) {
        return jobService.getJob(id);
    }

    @PostMapping("/{id}/assign")
    public JobResponse assignJob(@PathVariable UUID id, @Valid @RequestBody AssignJobRequest req) {
        return jobService.assignJob(id, req, currentUser.getCurrentUserId());
    }

    @PostMapping("/{id}/reschedule")
    public JobResponse rescheduleJob(@PathVariable UUID id, @Valid @RequestBody RescheduleJobRequest req) {
        return jobService.rescheduleJob(id, req, currentUser.getCurrentUserId());
    }

    @PostMapping("/{id}/close")
    public JobResponse closeJob(@PathVariable UUID id,
            @RequestBody(required = false) com.rjs.fsm.job.dto.CloseJobRequest req) {
        return jobService.closeJob(id, currentUser.getCurrentUserId(), req);
    }

    @PostMapping("/{id}/cancel")
    public JobResponse cancelJob(@PathVariable UUID id) {
        return jobService.cancelJob(id, currentUser.getCurrentUserId());
    }

    @GetMapping("/{id}/history")
    public List<JobHistoryResponse> getJobHistory(@PathVariable UUID id) {
        return jobService.getJobHistory(id);
    }
}
