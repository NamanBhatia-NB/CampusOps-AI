package com.campusops.repository;

import com.campusops.entity.LeadNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadNoteRepository extends JpaRepository<LeadNote, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author"})
    List<LeadNote> findByLeadIdOrderByCreatedAtDesc(Long leadId);
}
