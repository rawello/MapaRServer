package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.Building;
import com.rawello.maparserver.repository.BuildingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    @Autowired
    private BuildingRepository buildingRepository;

    @GetMapping
    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    @PostMapping
    public Building createBuilding(@RequestBody Building building) {
        return buildingRepository.save(building);
    }
}