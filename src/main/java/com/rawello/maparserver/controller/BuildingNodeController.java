package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.Building;
import com.rawello.maparserver.model.Node;
import com.rawello.maparserver.repository.BuildingRepository;
import com.rawello.maparserver.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

import java.util.Optional;

import java.util.Optional;

@RestController
@RequestMapping("/buildings")
public class BuildingNodeController {
    private static final Logger logger = LoggerFactory.getLogger(BuildingNodeController.class);

    private final BuildingRepository buildingRepository;
    private final NodeRepository nodeRepository;

    public BuildingNodeController(BuildingRepository buildingRepository, NodeRepository nodeRepository) {
        this.buildingRepository = buildingRepository;
        this.nodeRepository = nodeRepository;
    }

    @PostMapping("/{buildingId}/nodes")
    public ResponseEntity<?> createBuildingNode(
            @PathVariable Long buildingId,
            @RequestBody Node newNode) {


        Optional<Building> building = buildingRepository.findById(buildingId);
        if (!building.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        if (newNode.getModelPath() == null) {
            newNode.setModelPath("");
        }
        if (newNode.getTitle() == null) {
            newNode.setTitle("");
        }
        if (newNode.getNeighbors() == null) {
            newNode.setNeighbors("");
        }

        newNode.setBuilding(building.get());

        try {
            Node savedNode = nodeRepository.save(newNode);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedNode);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating node: " + e.getMessage());
        }
    }

    @PutMapping("/{buildingId}/nodes/{nodeId}")
    public ResponseEntity<?> updateBuildingNode(
            @PathVariable Long buildingId,
            @PathVariable Long nodeId,
            @RequestBody Node nodeUpdate) {


        Optional<Building> building = buildingRepository.findById(buildingId);
        if (!building.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Optional<Node> existingNode = nodeRepository.findById(nodeId);
        if (!existingNode.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Node node = existingNode.get();
        if (!buildingId.equals(node.getBuilding().getId())) {
            return ResponseEntity.badRequest().build();
        }

        node.setName(nodeUpdate.getName());
        node.setTranslation(nodeUpdate.getTranslation());
        node.setQuaternion(nodeUpdate.getQuaternion());
        node.setModelPath(nodeUpdate.getModelPath());
        node.setTitle(nodeUpdate.getTitle());
        node.setNeighbors(nodeUpdate.getNeighbors());

        Node savedNode = nodeRepository.save(node);

        return ResponseEntity.ok(savedNode);
    }

    @DeleteMapping("/{buildingId}/nodes/{nodeId}")
    public ResponseEntity<?> deleteBuildingNode(
            @PathVariable Long buildingId,
            @PathVariable Long nodeId) {


        Optional<Building> building = buildingRepository.findById(buildingId);
        if (!building.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Optional<Node> node = nodeRepository.findById(nodeId);
        if (!node.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        if (!buildingId.equals(node.get().getBuilding().getId())) {
            return ResponseEntity.badRequest().build();
        }

        nodeRepository.deleteById(nodeId);

        return ResponseEntity.ok().build();
    }
}
