package com.campusops.repository;

import com.campusops.entity.SheetSyncJob;
import com.campusops.entity.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SheetSyncJobRepository extends JpaRepository<SheetSyncJob, Long> {

    List<SheetSyncJob> findByStatusOrderByCreatedAtDesc(SyncStatus status);

    org.springframework.data.domain.Page<SheetSyncJob> findByStatusOrderByCreatedAtDesc(SyncStatus status, org.springframework.data.domain.Pageable pageable);

    List<SheetSyncJob> findByInitiatedByIdOrderByCreatedAtDesc(Long initiatedById);
}
