package com.bala.employeemanagement.controller;

import com.bala.employeemanagement.dto.ChatRequest;
import com.bala.employeemanagement.dto.ChatResponse;
import com.bala.employeemanagement.service.ChatService;
import com.bala.employeemanagement.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService; // to get userId from username

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequest request,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get logged-in user ID
            Long userId = userService.findByUsername(userDetails.getUsername()).getId();
            String reply = chatService.getGeminiResponse(request.getMessage(), userId);
            return ResponseEntity.ok(new ChatResponse(reply));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ChatResponse("Error: " + e.getMessage()));
        }
    }
}