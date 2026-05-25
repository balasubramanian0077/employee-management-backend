package com.bala.employeemanagement.service;

import com.bala.employeemanagement.model.Salary;
import com.bala.employeemanagement.model.SalaryStatus;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.repository.SalaryRepository;
import com.bala.employeemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SalaryService {

    @Autowired
    private SalaryRepository salaryRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<Salary> getSalaryForCurrentUser() {
        User currentUser = getCurrentUser();
        return salaryRepository.findByUser(currentUser);
    }

    public Salary assignSalary(Salary salary) {
        User user = userRepository.findById(salary.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Optional<Salary> existing = salaryRepository.findByUserAndMonthAndYear(user, salary.getMonth(), salary.getYear());
        if (existing.isPresent()) {
            throw new RuntimeException("Salary already assigned for this month");
        }
        salary.setUser(user);
        salary.setStatus(SalaryStatus.ASSIGNED);
        return salaryRepository.save(salary);
    }
    public List<Salary> getAllSalaries() {
        return salaryRepository.findAll();
    }
}