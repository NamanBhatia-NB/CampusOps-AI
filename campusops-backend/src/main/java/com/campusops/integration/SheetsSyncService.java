package com.campusops.integration;

import com.campusops.dto.LeadDTO;
import com.campusops.entity.SheetSyncJob;
import com.campusops.entity.User;
import com.campusops.entity.enums.SyncStatus;
import com.campusops.entity.enums.SyncType;
import com.campusops.exception.ResourceNotFoundException;
import com.campusops.repository.SheetSyncJobRepository;
import com.campusops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SheetsSyncService {

    private final SheetSyncJobRepository syncJobRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${app.worker.base-url:http://localhost:5000}")
    private String workerBaseUrl;

    @Transactional
    public Long exportLeads(List<LeadDTO> leads, Long initiatedById) {
        SheetSyncJob job = createJob(initiatedById, SyncType.LEADS);

        Map<String, Object> payload = new HashMap<>();
        payload.put("job_id", job.getId());
        payload.put("leads", leads);
        payload.put("spreadsheet_title", "CampusOps AI - Leads Export");

        try {
            // Send async-ish to Python worker
            log.info("Sending leads export request to worker: {} leads", leads.size());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    workerBaseUrl + "/api/sync/leads", 
                    payload, 
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                job.setStatus(SyncStatus.RUNNING);
            } else {
                job.setStatus(SyncStatus.FAILED);
                job.setErrorMessage("Worker returned error status");
                job.setCompletedAt(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Failed to call worker for leads export", e);
            job.setStatus(SyncStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
        }

        return syncJobRepository.save(job).getId();
    }

    @Transactional
    public Long exportReport(Map<String, Object> reportData, Long initiatedById) {
        SheetSyncJob job = createJob(initiatedById, SyncType.REPORT);

        Map<String, Object> payload = new HashMap<>();
        payload.put("job_id", job.getId());
        payload.put("report_data", reportData);
        payload.put("spreadsheet_title", "CampusOps AI - Report Export");

        try {
            log.info("Sending report export request to worker");
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    workerBaseUrl + "/api/sync/report", 
                    payload, 
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                job.setStatus(SyncStatus.RUNNING);
            } else {
                job.setStatus(SyncStatus.FAILED);
                job.setErrorMessage("Worker returned error status");
                job.setCompletedAt(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Failed to call worker for report export", e);
            job.setStatus(SyncStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
        }

        return syncJobRepository.save(job).getId();
    }

    @Transactional
    public void updateJobStatus(Long jobId, String status, String sheetUrl, int recordCount, String error) {
        SheetSyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("SheetSyncJob", "id", jobId));

        job.setStatus(SyncStatus.valueOf(status));
        job.setSheetUrl(sheetUrl);
        job.setRecordCount(recordCount);
        job.setErrorMessage(error);
        
        if (job.getStatus() == SyncStatus.COMPLETED || job.getStatus() == SyncStatus.FAILED) {
            job.setCompletedAt(LocalDateTime.now());
        }

        syncJobRepository.save(job);
        log.info("Updated sync job {}: status={}, sheet={}", jobId, status, sheetUrl);
    }

    @Transactional(readOnly = true)
    public Page<SheetSyncJob> getRecentJobs(Pageable pageable) {
        return syncJobRepository.findByStatusOrderByCreatedAtDesc(SyncStatus.COMPLETED, pageable);
    }

    private SheetSyncJob createJob(Long userId, SyncType type) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        
        SheetSyncJob job = new SheetSyncJob();
        job.setInitiatedBy(user);
        job.setSyncType(type);
        job.setStatus(SyncStatus.PENDING);
        job.setStartedAt(LocalDateTime.now());
        
        return syncJobRepository.save(job);
    }
}
