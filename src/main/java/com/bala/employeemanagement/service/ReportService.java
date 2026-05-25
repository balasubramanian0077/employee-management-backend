package com.bala.employeemanagement.service;

import com.bala.employeemanagement.model.Report;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.repository.ReportRepository;
import com.bala.employeemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public Report createReport(Report report) {
        User currentUser = getCurrentUser();
        report.setGeneratedBy(currentUser);
        report.setCreatedAt(LocalDateTime.now());   // add this line
        return reportRepository.save(report);
    }
    }
