package com.bala.employeemanagement.controller;

import com.bala.employeemanagement.model.Report;
import com.bala.employeemanagement.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "https://employee-interface.netlify.app", allowCredentials = "true")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Report> createReport(@RequestBody Report report) {
        return ResponseEntity.ok(reportService.createReport(report));
    }
}