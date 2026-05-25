package com.bala.employeemanagement.controller;

import com.bala.employeemanagement.model.Expense;
import com.bala.employeemanagement.model.ExpenseStatus;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.repository.ExpenseRepository;
import com.bala.employeemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    // Helper method to get current User from UserDetails
    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Existing: create expense with JSON (without file)
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Expense> createExpense(@RequestBody Expense expense,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        expense.setUser(currentUser);
        expense.setStatus(ExpenseStatus.PENDING);
        expense.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(expenseRepository.save(expense));
    }

    // NEW: create expense with file upload
    @PostMapping(value = "/create-with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> createExpenseWithFile(@RequestParam("amount") BigDecimal amount,
                                                   @RequestParam("reason") String reason,
                                                   @RequestParam("file") MultipartFile file,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Use absolute path to your project's uploads folder
            String projectRoot = System.getProperty("user.dir");
            String uploadDir = projectRoot + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = System.currentTimeMillis() + "_" + originalFilename;
            String filePath = uploadDir + fileName;

            file.transferTo(new File(filePath));

            User currentUser = getCurrentUser(userDetails);
            Expense expense = new Expense();
            expense.setUser(currentUser);
            expense.setAmount(amount);
            expense.setReason(reason);
            expense.setProofUrl("/uploads/" + fileName);
            expense.setStatus(ExpenseStatus.PENDING);
            expense.setCreatedAt(LocalDateTime.now());
            Expense saved = expenseRepository.save(expense);

            return ResponseEntity.ok(Map.of("message", "Expense submitted successfully", "expense", saved));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")   // only MANAGER can approve/reject
    public ResponseEntity<Expense> approveExpense(@PathVariable Long id, @RequestParam boolean approve) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        expense.setStatus(approve ? ExpenseStatus.APPROVED : ExpenseStatus.REJECTED);
        return ResponseEntity.ok(expenseRepository.save(expense));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Expense>> getExpenses(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        List<Expense> expenses;
        if (currentUser.getRole().name().equals("ADMIN")) {
            expenses = expenseRepository.findAll();
        } else {
            expenses = expenseRepository.findByUser(currentUser);
        }
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<Expense>> getPendingExpenses() {
        return ResponseEntity.ok(expenseRepository.findByStatus(ExpenseStatus.PENDING));
    }
}