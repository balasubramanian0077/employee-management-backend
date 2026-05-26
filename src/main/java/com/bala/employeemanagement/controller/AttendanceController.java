package com.bala.employeemanagement.controller;

import com.bala.employeemanagement.dto.AttendanceFilterDTO;
import com.bala.employeemanagement.dto.AttendanceStatsDTO;
import com.bala.employeemanagement.model.Attendance;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.service.AttendanceService;
import com.bala.employeemanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@CrossOrigin(origins = "https://employee-interface.netlify.app", allowCredentials = "true")
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private UserService userService;

    // Helper to get current User from UserDetails
    private User getCurrentUser(UserDetails userDetails) {
        return userService.findByUsername(userDetails.getUsername());
    }

    // Employee: Check-in
    @PostMapping("/check-in")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> checkIn(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User employee = getCurrentUser(userDetails);
            Attendance attendance = attendanceService.checkIn(employee);
            return ResponseEntity.ok(Map.of("message", "Checked in successfully", "attendance", attendance));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Employee: Check-out
    @PostMapping("/check-out")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> checkOut(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User employee = getCurrentUser(userDetails);
            Attendance attendance = attendanceService.checkOut(employee);
            return ResponseEntity.ok(Map.of("message", "Checked out successfully", "attendance", attendance));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Employee: Today's attendance
    @GetMapping("/today")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> getTodayAttendance(@AuthenticationPrincipal UserDetails userDetails) {
        User employee = getCurrentUser(userDetails);
        Attendance attendance = attendanceService.getTodayAttendance(employee);
        return ResponseEntity.ok(attendance);
    }

    // Employee: My history
    @GetMapping("/my-history")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> getMyHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User employee = getCurrentUser(userDetails);
        List<Attendance> history = attendanceService.getEmployeeHistory(employee);
        return ResponseEntity.ok(history);
    }

    // Employee: My monthly stats
    @GetMapping("/my-stats")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> getMyStats(@AuthenticationPrincipal UserDetails userDetails) {
        User employee = getCurrentUser(userDetails);
        AttendanceStatsDTO stats = attendanceService.getEmployeeMonthlyStats(employee);
        return ResponseEntity.ok(stats);
    }

    // Admin/Manager: Get all attendances with filters
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<Attendance>> getAllAttendances(AttendanceFilterDTO filter) {
        List<Attendance> attendances = attendanceService.getAllAttendances(filter);
        return ResponseEntity.ok(attendances);
    }

    // Admin/Manager: Get attendance of a specific employee
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<Attendance>> getEmployeeAttendance(@PathVariable Long employeeId) {
        List<Attendance> attendances = attendanceService.getEmployeeAttendance(employeeId);
        return ResponseEntity.ok(attendances);
    }

    // Export attendance to Excel
    @GetMapping("/export-excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportAttendanceToExcel(AttendanceFilterDTO filter) {
        List<Attendance> attendances = attendanceService.getAllAttendances(filter);
        byte[] excelData = attendanceService.generateAttendanceExcel(attendances);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "attendance_report.xlsx");
        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }
}