package com.bala.employeemanagement.service;

import com.bala.employeemanagement.model.AttendanceStatus;
import com.bala.employeemanagement.model.LeaveStatus;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.repository.LeaveRepository;
import com.bala.employeemanagement.repository.UserRepository;
import com.bala.employeemanagement.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.util.*;

@Service
public class ChatService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    // Fallback: directly answer based on database (no AI)
    private String getFallbackResponse(String userMessage, long approvedLeaves, long presentDays) {
        String msg = userMessage.toLowerCase();
        if (msg.contains("leave balance") || msg.contains("how many leaves")) {
            return "You have taken " + approvedLeaves + " approved leaves this year. Total annual leave allowed is 20 days.";
        } else if (msg.contains("attendance") || msg.contains("present")) {
            return "You have been present for " + presentDays + " days this month.";
        } else {
            return "I'm currently offline. Please check your dashboard for leave/attendance information.";
        }
    }

    public String getGeminiResponse(String userMessage, Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            long approvedLeaves = leaveRepository.countByUserAndStatus(user, LeaveStatus.APPROVED);
            long presentDays = attendanceRepository.countByEmployeeAndAttendanceDateBetweenAndStatus(
                user, LocalDate.now().withDayOfMonth(1), LocalDate.now(), AttendanceStatus.PRESENT);
            long totalAllowed = 20; // company policy

            // Build a strict system prompt
            String systemPrompt = String.format(
                "You are an HR assistant. Use ONLY the data below to answer. Do not invent dates or calculate anything not given.\n\n" +
                "Employee: %s\n" +
                "Approved leaves taken this year: %d out of %d\n" +
                "Present days this month (so far): %d\n\n" +
                "Answer the employee's question concisely (one sentence). If the question is not about leave or attendance, say: 'Please contact HR for that.'",
                user.getName(), approvedLeaves, totalAllowed, presentDays
            );
            String userPrompt = userMessage;

            String aiResponse = callGroqApi(systemPrompt, userPrompt);
            return aiResponse;

        } catch (Exception e) {
            // Fallback on any error
            try {
                User user = userRepository.findById(userId).orElse(null);
                long approvedLeaves = (user != null) ? leaveRepository.countByUserAndStatus(user, LeaveStatus.APPROVED) : 0;
                long presentDays = (user != null) ? attendanceRepository.countByEmployeeAndAttendanceDateBetweenAndStatus(
                    user, LocalDate.now().withDayOfMonth(1), LocalDate.now(), AttendanceStatus.PRESENT) : 0;
                return getFallbackResponse(userMessage, approvedLeaves, presentDays);
            } catch (Exception ex) {
                return "I'm sorry, I'm having trouble connecting. Please try again later.";
            }
        }
    }

    private String callGroqApi(String systemPrompt, String userPrompt) {
        String url = apiUrl;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3); // lower temperature = more factual
        requestBody.put("max_tokens", 150);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                Map body = response.getBody();
                List<Map> choices = (List<Map>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map choice = choices.get(0);
                    Map message = (Map) choice.get("message");
                    String content = (String) message.get("content");
                    return content.trim();
                }
            }
            return "I couldn't understand the response.";
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return "The AI service is currently busy. Please wait a moment and try again.";
            }
            return "Error: " + e.getMessage();
        }
    }
}