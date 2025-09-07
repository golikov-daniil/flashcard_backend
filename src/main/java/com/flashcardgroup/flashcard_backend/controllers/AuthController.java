package com.flashcardgroup.flashcard_backend.controllers;

import com.flashcardgroup.flashcard_backend.model.User;
import com.flashcardgroup.flashcard_backend.service.DataService;
import com.flashcardgroup.flashcard_backend.security.JwtService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin
@Validated
public class AuthController {

    private final DataService dataService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(DataService dataService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.dataService = dataService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest req
    ) {
        if (req.username == null || req.email == null || req.password == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }
        try {
            Optional<User> existing = dataService.getUserByUsername(req.username);
            if (existing.isPresent()) {
                return ResponseEntity.status(409).body("Username already exists");
            }
            User user = new User();
            user.setUsername(req.username);
            user.setEmail(req.email);
            user.setPasswordHash(passwordEncoder.encode(req.password));
            dataService.addUser(user);
            return ResponseEntity.status(201).body("User registered successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error registering user");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.username == null || req.password == null) {
            return ResponseEntity.badRequest().body("Username and password are required.");
        }
        try {
            Optional<User> userOpt = dataService.getUserByUsername(req.username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Invalid credentials.");
            }
            User user = userOpt.get();
            if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
                return ResponseEntity.status(401).body("Invalid credentials.");
            }
            // 1 year in seconds
            String token = jwtService.generateToken(user.getUserId().longValue(), 60L * 60 * 24 * 365);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Server error during login.");
        }
    }

    public static class RegisterRequest {
        @NotBlank @Size(min = 3, max = 50)
        public String username;
        @NotBlank @Email
        public String email;
        @NotBlank @Size(min = 6, max = 200)
        public String password;
    }

    public static class LoginRequest {
        @NotBlank
        public String username;
        @NotBlank
        public String password;
    }
}