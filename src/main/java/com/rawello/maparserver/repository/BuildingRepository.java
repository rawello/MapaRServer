package com.rawello.maparserver.repository;

import com.rawello.maparserver.model.Building;
import com.rawello.maparserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    List<Building> findByUser(User user);

    boolean existsById(Long id);
}