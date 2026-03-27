package com.rjs.fsm.job;

import com.rjs.fsm.job.dto.FollowUpRequest;
import com.rjs.fsm.job.dto.JobHistoryResponse;
import com.rjs.fsm.job.dto.JobResponse;
import com.rjs.fsm.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tech/jobs")
public class TechJobController {

    private final JobService jobService;
    private final CurrentUserProvider currentUser;

    public TechJobController(JobService jobService, CurrentUserProvider currentUser) {
        this.jobService = jobService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<JobResponse> listMyJobs(@RequestParam(defaultValue = "false") boolean activeOnly) {
        UUID techId = currentUser.getCurrentUserId();
        if (activeOnly) return jobService.listMyActiveJobs(techId);
        return jobService.listMyJobs(techId);
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable UUID id) {
        UUID techId = currentUser.getCurrentUserId();
        JobResponse job = jobService.getJob(id);
        if (job.getAssignedToId() == null || !techId.equals(job.getAssignedToId())) {
            throw new com.rjs.fsm.exception.ForbiddenException("Anda tidak memiliki akses ke job ini");
        }
        return job;
    }

    @PostMapping("/{id}/transit")
    public JobResponse startTransit(@PathVariable UUID id) {
        return jobService.startTransit(id, currentUser.getCurrentUserId());
    }

    @PostMapping("/{id}/start")
    public JobResponse startJob(@PathVariable UUID id) {
        return jobService.startJob(id, currentUser.getCurrentUserId());
    }

    @PostMapping("/{id}/finish")
    public JobResponse finishJob(@PathVariable UUID id) {
        return jobService.finishJob(id, currentUser.getCurrentUserId());
    }

    @PostMapping("/{id}/followup")
    public JobResponse markFollowUp(@PathVariable UUID id, @Valid @RequestBody FollowUpRequest req) {
        return jobService.markFollowUp(id, currentUser.getCurrentUserId(), req.getReason());
    }

    @GetMapping("/{id}/history")
    public List<JobHistoryResponse> getJobHistory(@PathVariable UUID id) {
        UUID techId = currentUser.getCurrentUserId();
        JobResponse job = jobService.getJob(id);
        if (job.getAssignedToId() == null || !techId.equals(job.getAssignedToId())) {
            throw new com.rjs.fsm.exception.ForbiddenException("Anda tidak memiliki akses ke job ini");
        }
        return jobService.getJobHistory(id);
    }
}
