package com.bala.employeemanagement.service;

import com.bala.employeemanagement.model.Leave;
import com.bala.employeemanagement.model.LeaveStatus;
import com.bala.employeemanagement.model.Role;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.repository.LeaveRepository;
import com.bala.employeemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Employee: submit leave request
    @Transactional
    public Leave createLeave(Leave leave) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.EMPLOYEE) {
            throw new RuntimeException("Only employees can request leave");
        }

        LocalDate today = LocalDate.now();
        // 1. Start date cannot be in the past
        if (leave.getStartDate().isBefore(today)) {
            throw new RuntimeException("Start date cannot be a past date");
        }
        // 2. End date must be >= start date
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new RuntimeException("End date must be after or equal to start date");
        }

        leave.setUser(currentUser);
        leave.setStatus(LeaveStatus.PENDING);
        leave.setCreatedAt(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    // Admin/Manager: approve or reject leave
    @Transactional
    public Leave approveLeave(Long id, boolean approve) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found"));
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.MANAGER) {
            throw new RuntimeException("Unauthorized");
        }
        leave.setStatus(approve ? LeaveStatus.APPROVED : LeaveStatus.REJECTED);
        return leaveRepository.save(leave);
    }

    // Get all pending leaves (for admin/manager)
    public List<Leave> getPendingLeaves() {
        return leaveRepository.findByStatus(LeaveStatus.PENDING);
    }

    // Get leaves for the current user (admin sees all)
    public List<Leave> getLeavesForCurrentUser() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return leaveRepository.findAll();
        } else {
            return leaveRepository.findByUser(currentUser);
        }
    }
}