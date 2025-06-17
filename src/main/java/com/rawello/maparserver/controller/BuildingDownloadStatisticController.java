package com.rawello.maparserver.controller;

import com.rawello.maparserver.model.Building;
import com.rawello.maparserver.model.BuildingDownloadStatistic;
import com.rawello.maparserver.repository.BuildingRepository;
import com.rawello.maparserver.service.BuildingDownloadStatisticService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/building-download-statistics")
public class BuildingDownloadStatisticController {
    private final BuildingDownloadStatisticService statisticService;
    private final BuildingRepository buildingRepository;

    public BuildingDownloadStatisticController(BuildingDownloadStatisticService statisticService,
            BuildingRepository buildingRepository) {
        this.statisticService = statisticService;
        this.buildingRepository = buildingRepository;
    }

    @PostMapping("/increment/{buildingId}")
    public ResponseEntity<Void> incrementDownload(@PathVariable Long buildingId) {
        Optional<Building> buildingOpt = buildingRepository.findById(buildingId);
        if (buildingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        statisticService.incrementDownloadCount(buildingOpt.get());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/day/{buildingId}")
    public ResponseEntity<List<BuildingDownloadStatistic>> getDayStats(
            @PathVariable Long buildingId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(statisticService.getStatisticsForDay(buildingId, date));
    }

    @GetMapping("/week/{buildingId}")
    public ResponseEntity<List<BuildingDownloadStatistic>> getWeekStats(
            @PathVariable Long buildingId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(statisticService.getStatisticsForWeek(buildingId, date));
    }

    @GetMapping("/month/{buildingId}")
    public ResponseEntity<List<BuildingDownloadStatistic>> getMonthStats(
            @PathVariable Long buildingId,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        return ResponseEntity.ok(statisticService.getStatisticsForMonth(buildingId, year, month));
    }
}
