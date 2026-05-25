package com.bala.employeemanagement.service;

import com.bala.employeemanagement.model.Expense;
import com.bala.employeemanagement.model.ExpenseStatus;
import com.bala.employeemanagement.model.Role;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.repository.ExpenseRepository;
import com.bala.employeemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Expense createExpense(Expense expense) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.EMPLOYEE) {
            throw new RuntimeException("Only employees can submit expenses");
        }
        expense.setUser(currentUser);
        expense.setStatus(ExpenseStatus.PENDING);
        return expenseRepository.save(expense);
    }

    // Original approve method – no message parameter
    public Expense approveExpense(Long id, boolean approve) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.MANAGER) {   // only MANAGER
            throw new RuntimeException("Only managers can approve/reject expenses");
        }
        expense.setStatus(approve ? ExpenseStatus.APPROVED : ExpenseStatus.REJECTED);
        return expenseRepository.save(expense);
    }

    public List<Expense> getExpensesForCurrentUser() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return expenseRepository.findAll();
        } else {
            return expenseRepository.findByUser(currentUser);
        }
    }

    public List<Expense> getPendingExpenses() {
        return expenseRepository.findByStatus(ExpenseStatus.PENDING);
    }
}