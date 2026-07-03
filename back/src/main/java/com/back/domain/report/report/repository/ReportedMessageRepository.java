package com.back.domain.report.report.repository;

import com.back.domain.report.report.entity.ReportedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportedMessageRepository extends JpaRepository<ReportedMessage, UUID> {
    List<ReportedMessage> findByReportIdOrderBySentAtAsc(UUID reportId);
}