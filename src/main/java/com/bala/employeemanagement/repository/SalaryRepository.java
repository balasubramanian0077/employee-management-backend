package com.bala.employeemanagement.repository;

import com.bala.employeemanagement.model.Salary;
import com.bala.employeemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SalaryRepository extends JpaRepository<Salary, Long> {
    Optional<Salary> findByUserAndMonthAndYear(User user, int month, int year);
    List<Salary> findByUser(User user);
}