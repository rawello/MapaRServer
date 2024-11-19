package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.Building;
import com.rawello.maparserver.model.Node;
import com.rawello.maparserver.model.User;
import com.rawello.maparserver.repository.BuildingRepository;
import com.rawello.maparserver.repository.NodeRepository;
import com.rawello.maparserver.repository.UserRepository;
import com.rawello.maparserver.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://26.239.171.49:3000")
@RequestMapping("/api/buildings")
public class BuildingController {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private static final Logger logger = LoggerFactory.getLogger(BuildingController.class);

    @GetMapping
    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    @GetMapping("/{id}/nodes")
    public ResponseEntity<List<Node>> getNodesForBuilding(@PathVariable Long id) {
        Optional<Building> building = buildingRepository.findById(id);
        if (building.isPresent()) {
            List<Node> nodes = nodeRepository.findByBuildingId(id);
            return ResponseEntity.ok(nodes);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<Building> createBuilding(@RequestBody Building building, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null || "anonymous".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Creating building for user: {}", username);

        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        building.setUser(user);
        return ResponseEntity.ok(buildingRepository.save(building));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Building> getBuildingById(@PathVariable Long id, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Getting building by id: {} for user: {}", id, username);

        Optional<Building> building = buildingRepository.findById(id);
        if (building.isPresent() && building.get().getUser().getId().equals(getCurrentUserId(username))) {
            return ResponseEntity.ok(building.get());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private Long getCurrentUserId(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return user.getId();
    }

    @GetMapping("/{id}/is-owner")
    public ResponseEntity<Boolean> isBuildingOwner(@PathVariable Long id, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null) {
            return ResponseEntity.ok(false);
        }

        logger.info("Checking if user: {} is owner of building: {}", username, id);

        User user = userRepository.findByUsername(username);
        Optional<Building> building = buildingRepository.findById(id);
        boolean isOwner = building.isPresent() && building.get().getUser().getId().equals(user.getId());
        return ResponseEntity.ok(isOwner);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Building> updateBuilding(@PathVariable Long id, @RequestBody Building building, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null || "anonymous".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Updating building by id: {} for user: {}", id, username);

        if (!buildingRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Building> existingBuilding = buildingRepository.findById(id);
        if (existingBuilding.isPresent() && existingBuilding.get().getUser().getId().equals(getCurrentUserId(username))) {
            Building updatedBuilding = existingBuilding.get();
            updatedBuilding.setName(building.getName());
            updatedBuilding.setAddress(building.getAddress());
            return ResponseEntity.ok(buildingRepository.save(updatedBuilding));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBuilding(@PathVariable Long id, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null || "anonymous".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Deleting building by id: {} for user: {}", id, username);

        if (!buildingRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Building> building = buildingRepository.findById(id);
        if (building.isPresent() && building.get().getUser().getId().equals(getCurrentUserId(username))) {
            buildingRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<Building>> getBuildingsForUser(HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Getting buildings for user: {}", username);

        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<Building> buildings = buildingRepository.findByUser(user);
        return ResponseEntity.ok(buildings);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}