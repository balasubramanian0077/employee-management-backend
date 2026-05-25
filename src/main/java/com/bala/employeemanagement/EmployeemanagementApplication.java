package com.bala.employeemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EmployeemanagementApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(EmployeemanagementApplication.class, args);
        } catch (Exception e) {
            // This will print the full stack trace of the error
            e.printStackTrace();
            // Optionally, throw the exception so the application still fails and doesn't start incorrectly
            throw new RuntimeException(e);
        }
    }
}