package com.back.domain.report.report.repository;

import com.back.domain.report.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
}