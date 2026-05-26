package com.bala.employeemanagement.controller;

import com.bala.employeemanagement.model.Salary;
import com.bala.employeemanagement.service.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "https://employee-interface.netlify.app", allowCredentials = "true")
@RestController
@RequestMapping("/api/salary")
public class SalaryController {

    @Autowired
    private SalaryService salaryService;

    @GetMapping
    public ResponseEntity<?> getSalary() {
        // Returns salary for the logged-in user (employee/manager)
        return ResponseEntity.ok(salaryService.getSalaryForCurrentUser());
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Salary> assignSalary(@RequestBody Salary salary) {
        return ResponseEntity.ok(salaryService.assignSalary(salary));
    }
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Salary>> getAllSalaries() {
        return ResponseEntity.ok(salaryService.getAllSalaries());
    }
}