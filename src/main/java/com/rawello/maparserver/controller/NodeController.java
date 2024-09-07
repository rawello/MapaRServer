package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.Node;
import com.rawello.maparserver.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    @Autowired
    private NodeRepository nodeRepository;

    @GetMapping
    public List<Node> getAllNodes() {
        return nodeRepository.findAll();
    }

    @PostMapping
    public Node createNode(@RequestBody Node node) {
        return nodeRepository.save(node);
    }

    @DeleteMapping("/{id}")
    public void deleteNode(@PathVariable Long id) {
        nodeRepository.deleteById(id);
    }

    @PutMapping("/api/nodes/{id}")
    public ResponseEntity<Node> updateNode(@PathVariable Long id, @RequestBody Node node) {
        Optional<Node> existingNode = nodeRepository.findById(id);
        if (existingNode.isPresent()) {
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
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}