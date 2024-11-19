package com.rawello.maparserver.repository;

import com.rawello.maparserver.model.Node;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByBuildingId(Long buildingId);
    Optional<Node> findTopByOrderByIdDesc();

    boolean existsById(Long id);
}