package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.Building;
import com.rawello.maparserver.model.Node;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://26.239.171.49:3000")
@RequestMapping("/api/nodes")
public class NodeController {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private static final Logger logger = LoggerFactory.getLogger(NodeController.class);

    @GetMapping
    public List<Node> getAllNodes() {
        return nodeRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Node> createNode(@RequestBody Node node, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null || "anonymous".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Creating node for user: {}", username);

        return ResponseEntity.ok(nodeRepository.save(node));
    }

    @GetMapping("/last-id")
    public ResponseEntity<Long> getLastNodeId() {
        Optional<Node> lastNode = nodeRepository.findTopByOrderByIdDesc();
        return lastNode.map(node -> ResponseEntity.ok(node.getId()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable Long id, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null || "anonymous".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Deleting node by id: {} for user: {}", id, username);

        if (!nodeRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Node> node = nodeRepository.findById(id);
        if (node.isPresent() && node.get().getBuilding().getUser().getId().equals(getCurrentUserId(username))) {
            nodeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Node> updateNode(@PathVariable Long id, @RequestBody Node node, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null || "anonymous".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Updating node by id: {} for user: {}", id, username);

        if (!nodeRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Node> existingNode = nodeRepository.findById(id);
        if (existingNode.isPresent() && existingNode.get().getBuilding().getUser().getId().equals(getCurrentUserId(username))) {
            Node updatedNode = existingNode.get();
            updatedNode.setName(node.getName());
            updatedNode.setTranslation(node.getTranslation());
            updatedNode.setQuaternion(node.getQuaternion());
            updatedNode.setModelPath(node.getModelPath());
            updatedNode.setTitle(node.getTitle());
            updatedNode.setNeighbors(node.getNeighbors());
            nodeRepository.save(updatedNode);
            return new ResponseEntity<>(updatedNode, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<Node>> getNodesForBuilding(@PathVariable Long buildingId, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(getJwtFromRequest(request));
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Getting nodes for building: {} for user: {}", buildingId, username);

        Optional<Building> building = buildingRepository.findById(buildingId);
        if (building.isPresent() && building.get().getUser().getId().equals(getCurrentUserId(username))) {
            List<Node> nodes = nodeRepository.findByBuildingId(buildingId);
            return ResponseEntity.ok(nodes);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private Long getCurrentUserId(String username) {
        return userRepository.findByUsername(username).getId();
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}