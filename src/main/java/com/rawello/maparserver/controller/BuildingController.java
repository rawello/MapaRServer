package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.Building;
import com.rawello.maparserver.model.Node;
import com.rawello.maparserver.model.User;
import com.rawello.maparserver.repository.BuildingRepository;
import com.rawello.maparserver.repository.NodeRepository;
import com.rawello.maparserver.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {
    private static final Logger logger = LoggerFactory.getLogger(BuildingController.class);

    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final NodeRepository nodeRepository;

    public BuildingController(BuildingRepository buildingRepository,
            UserRepository userRepository,
            NodeRepository nodeRepository) {
        this.buildingRepository = buildingRepository;
        this.userRepository = userRepository;
        this.nodeRepository = nodeRepository;
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping
    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    @CrossOrigin(origins = "http://localhost:3000")
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

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping
    public ResponseEntity<Building> createBuilding(@RequestBody Building building) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymous".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        building.setUser(user);
        return ResponseEntity.ok(buildingRepository.save(building));
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/{id}")
    public ResponseEntity<Building> getBuildingById(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("GetBuildingById - Auth details: name={}, isAuthenticated={}, authorities={}",
                authentication != null ? authentication.getName() : "null",
                authentication != null ? authentication.isAuthenticated() : "null",
                authentication != null ? authentication.getAuthorities() : "null");

        Optional<Building> building = buildingRepository.findById(id);
        if (!building.isPresent()) {
            logger.warn("Building not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }

        Building foundBuilding = building.get();
        logger.info("Found building: id={}, name={}, ownerId={}",
                foundBuilding.getId(),
                foundBuilding.getName(),
                foundBuilding.getUser() != null ? foundBuilding.getUser().getId() : "null");

        Long currentUserId = getCurrentUserId();
        logger.info("Current user id: {}", currentUserId);

        if (foundBuilding.getUser() == null) {
            logger.info("Building is public, allowing access");
            return ResponseEntity.ok(foundBuilding);
        }

        if (currentUserId != null && currentUserId.equals(foundBuilding.getUser().getId())) {
            logger.info("User is owner, allowing access");
            return ResponseEntity.ok(foundBuilding);
        }

        logger.warn("Access denied - User {} is not the owner of building {}", currentUserId, id);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    private boolean isCurrentUserOwner(Building building) {
        if (building == null || building.getUser() == null) {
            logger.debug("Building or building owner is null");
            return false;
        }

        Long currentUserId = getCurrentUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(building.getUser().getId());
        logger.debug("Checking ownership: currentUserId={}, buildingOwnerId={}, isOwner={}",
                currentUserId, building.getUser().getId(), isOwner);
        return isOwner;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            logger.debug("No authentication found");
            return null;
        }

        if (!authentication.isAuthenticated()) {
            logger.debug("User is not authenticated");
            return null;
        }

        if ("anonymous".equals(authentication.getName())) {
            logger.debug("Anonymous user");
            return null;
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            logger.warn("User not found in database: {}", authentication.getName());
            return null;
        }

        logger.debug("Current user found: id={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @GetMapping("/{id}/is-owner")
    public ResponseEntity<Boolean> isBuildingOwner(@PathVariable Long id) {
        Optional<Building> building = buildingRepository.findById(id);
        return ResponseEntity.ok(building.isPresent() && isCurrentUserOwner(building.get()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Building> updateBuilding(@PathVariable Long id, @RequestBody Building building) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("UpdateBuilding - Auth details: name={}, isAuthenticated={}, authorities={}",
                authentication != null ? authentication.getName() : "null",
                authentication != null ? authentication.isAuthenticated() : "null",
                authentication != null ? authentication.getAuthorities() : "null");

        Optional<Building> existingBuilding = buildingRepository.findById(id);
        if (!existingBuilding.isPresent()) {
            logger.warn("Building not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }

        Building foundBuilding = existingBuilding.get();
        logger.info("Found building to update: id={}, name={}, ownerId={}",
                foundBuilding.getId(),
                foundBuilding.getName(),
                foundBuilding.getUser() != null ? foundBuilding.getUser().getId() : "null");

        if (!isCurrentUserOwner(foundBuilding)) {
            logger.warn("Update denied - User is not the owner of building {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        foundBuilding.setName(building.getName());
        foundBuilding.setAddress(building.getAddress());
        Building savedBuilding = buildingRepository.save(foundBuilding);
        logger.info("Building updated successfully: id={}", savedBuilding.getId());
        return ResponseEntity.ok(savedBuilding);
    }
}