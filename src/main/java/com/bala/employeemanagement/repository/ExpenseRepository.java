package com.bala.employeemanagement.repository;

import com.bala.employeemanagement.model.Expense;
import com.bala.employeemanagement.model.ExpenseStatus;
import com.bala.employeemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUser(User user);
    List<Expense> findByStatus(ExpenseStatus status);
}