package com.bala.employeemanagement.controller;

import com.bala.employeemanagement.model.Leave;
import com.bala.employeemanagement.service.LeaveService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping
    public ResponseEntity<Leave> createLeave(@RequestBody Leave leave) {
        return ResponseEntity.ok(leaveService.createLeave(leave));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Leave> approveLeave(@PathVariable Long id, @RequestParam boolean approve) {
        return ResponseEntity.ok(leaveService.approveLeave(id, approve));
    }
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<Leave>> getPendingLeaves() {
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }
    @GetMapping
    public ResponseEntity<List<Leave>> getLeavesForCurrentUser() {
        return ResponseEntity.ok(leaveService.getLeavesForCurrentUser());
    }
}