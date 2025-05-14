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

        logger.debug("Creating new node in building {}", buildingId);

        Optional<Building> building = buildingRepository.findById(buildingId);
        if (!building.isPresent()) {
            logger.warn("Building {} not found", buildingId);
            return ResponseEntity.notFound().build();
        }

        // Устанавливаем обязательные значения по умолчанию
        if (newNode.getModelPath() == null) {
            newNode.setModelPath(""); // Пустая строка вместо null
        }
        if (newNode.getTitle() == null) {
            newNode.setTitle("");
        }
        if (newNode.getNeighbors() == null) {
            newNode.setNeighbors("");
        }

        // Устанавливаем связь с зданием
        newNode.setBuilding(building.get());

        try {
            // Сохраняем новый узел
            Node savedNode = nodeRepository.save(newNode);
            logger.info("Successfully created node {} in building {}", savedNode.getId(), buildingId);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedNode);
        } catch (Exception e) {
            logger.error("Error creating node in building {}: {}", buildingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating node: " + e.getMessage());
        }
    }

    @PutMapping("/{buildingId}/nodes/{nodeId}")
    public ResponseEntity<?> updateBuildingNode(
            @PathVariable Long buildingId,
            @PathVariable Long nodeId,
            @RequestBody Node nodeUpdate) {

        logger.debug("Updating node {} in building {}", nodeId, buildingId);

        Optional<Building> building = buildingRepository.findById(buildingId);
        if (!building.isPresent()) {
            logger.warn("Building {} not found", buildingId);
            return ResponseEntity.notFound().build();
        }

        Optional<Node> existingNode = nodeRepository.findById(nodeId);
        if (!existingNode.isPresent()) {
            logger.warn("Node {} not found", nodeId);
            return ResponseEntity.notFound().build();
        }

        Node node = existingNode.get();
        if (!buildingId.equals(node.getBuilding().getId())) {
            logger.warn("Node {} does not belong to building {}", nodeId, buildingId);
            return ResponseEntity.badRequest().build();
        }

        // Обновляем поля узла
        node.setName(nodeUpdate.getName());
        node.setTranslation(nodeUpdate.getTranslation());
        node.setQuaternion(nodeUpdate.getQuaternion());
        node.setModelPath(nodeUpdate.getModelPath());
        node.setTitle(nodeUpdate.getTitle());
        node.setNeighbors(nodeUpdate.getNeighbors());

        Node savedNode = nodeRepository.save(node);
        logger.info("Successfully updated node {} in building {}", nodeId, buildingId);

        return ResponseEntity.ok(savedNode);
    }

    @DeleteMapping("/{buildingId}/nodes/{nodeId}")
    public ResponseEntity<?> deleteBuildingNode(
            @PathVariable Long buildingId,
            @PathVariable Long nodeId) {

        logger.debug("Deleting node {} from building {}", nodeId, buildingId);

        Optional<Building> building = buildingRepository.findById(buildingId);
        if (!building.isPresent()) {
            logger.warn("Building {} not found", buildingId);
            return ResponseEntity.notFound().build();
        }

        Optional<Node> node = nodeRepository.findById(nodeId);
        if (!node.isPresent()) {
            logger.warn("Node {} not found", nodeId);
            return ResponseEntity.notFound().build();
        }

        if (!buildingId.equals(node.get().getBuilding().getId())) {
            logger.warn("Node {} does not belong to building {}", nodeId, buildingId);
            return ResponseEntity.badRequest().build();
        }

        nodeRepository.deleteById(nodeId);
        logger.info("Successfully deleted node {} from building {}", nodeId, buildingId);

        return ResponseEntity.ok().build();
    }
}
