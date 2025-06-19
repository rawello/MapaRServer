package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.Building;
import com.rawello.maparserver.model.Node;
import com.rawello.maparserver.model.User;
import com.rawello.maparserver.repository.BuildingRepository;
import com.rawello.maparserver.repository.NodeRepository;
import com.rawello.maparserver.repository.UserRepository;
import com.rawello.maparserver.service.BuildingDownloadStatisticService;
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
    private final BuildingDownloadStatisticService buildingDownloadStatisticService;

    public BuildingController(BuildingRepository buildingRepository,
            UserRepository userRepository,
            NodeRepository nodeRepository,
            BuildingDownloadStatisticService buildingDownloadStatisticService) {
        this.buildingRepository = buildingRepository;
        this.userRepository = userRepository;
        this.nodeRepository = nodeRepository;
        this.buildingDownloadStatisticService = buildingDownloadStatisticService;
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
            buildingDownloadStatisticService.incrementDownloadCount(building.get());
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


        Optional<Building> building = buildingRepository.findById(id);
        if (!building.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Building foundBuilding = building.get();


        buildingDownloadStatisticService.incrementDownloadCount(foundBuilding);

        Long currentUserId = getCurrentUserId();

        if (foundBuilding.getUser() == null) {
            return ResponseEntity.ok(foundBuilding);
        }

        if (currentUserId != null && currentUserId.equals(foundBuilding.getUser().getId())) {
            return ResponseEntity.ok(foundBuilding);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    private boolean isCurrentUserOwner(Building building) {
        if (building == null || building.getUser() == null) {
            return false;
        }

        Long currentUserId = getCurrentUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(building.getUser().getId());

        return isOwner;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        if (!authentication.isAuthenticated()) {
            return null;
        }

        if ("anonymous".equals(authentication.getName())) {
            return null;
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            return null;
        }

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

        Optional<Building> existingBuilding = buildingRepository.findById(id);
        if (!existingBuilding.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Building foundBuilding = existingBuilding.get();

        if (!isCurrentUserOwner(foundBuilding)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        foundBuilding.setName(building.getName());
        foundBuilding.setAddress(building.getAddress());
        Building savedBuilding = buildingRepository.save(foundBuilding);

        return ResponseEntity.ok(savedBuilding);
    }
}