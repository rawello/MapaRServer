package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.User;
import com.rawello.maparserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin(origins = "http://192.168.58.14:3000")
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !Objects.equals(authentication.getName(), "admin_rawello_admin")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !Objects.equals(authentication.getName(), "admin_rawello_admin")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("/favorites")
    public ResponseEntity<String> updateFavorites(@RequestBody List<Long> favoriteBuildings) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            User user = userRepository.findByUsername(authentication.getName());
            user.setFavoriteBuildings(favoriteBuildings);
            userRepository.updateFavoriteBuildings(user.getId(),favoriteBuildings);
            userRepository.save(user);
            return ResponseEntity.ok("{\"status\": \"success\"}");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Failed to update favorites\", \"message\":}" + " " + ex.getMessage());
        }
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<Long>> getFavorites() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByUsername(authentication.getName());
        if(user == null) {
            return ResponseEntity.ok(new ArrayList<Long>());
        }
        return ResponseEntity.ok(user.getFavoriteBuildings());
    }

    @GetMapping("/is-anonymous")
    public ResponseEntity<Boolean> isAnonymousUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = authentication != null && authentication.isAuthenticated() && "anonymous".equals(authentication.getName());
        return ResponseEntity.ok(isAnonymous);
    }
}