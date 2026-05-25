package com.bala.employeemanagement.repository;

import com.bala.employeemanagement.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}