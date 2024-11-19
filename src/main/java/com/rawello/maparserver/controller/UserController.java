package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.User;
import com.rawello.maparserver.repository.UserRepository;
import com.rawello.maparserver.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin(origins = "http://26.239.171.49:3000")
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

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

        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }

        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("/favorites")
    public ResponseEntity<String> updateFavorites(@RequestBody List<Long> favoriteBuildings, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null || "anonymous".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Updating favorites for user: {}", username);

        try {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                logger.error("User not found for username: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"User not found\"}");
            }

            userRepository.deleteAllFavoritesByUserId(user.getId());

            for (Long buildingId : favoriteBuildings) {
                userRepository.addFavoriteBuilding(user.getId(), buildingId);
            }

            return ResponseEntity.ok("{\"status\": \"success\"}");
        } catch (Exception ex) {
            logger.error("Error updating favorites", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Failed to update favorites\", \"message\": \"" + ex.getMessage() + "\"}");
        }
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<Long>> getFavorites(HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null || "anonymous".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Getting favorites for user: {}", username);

        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userRepository.findFavoriteBuildingsByUserId(user.getId()));
    }

    @GetMapping("/is-anonymous")
    public ResponseEntity<Boolean> isAnonymousUser(HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        boolean isAnonymous = username != null && "anonymous".equals(username);
        return ResponseEntity.ok(isAnonymous);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}