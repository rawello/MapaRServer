package com.rawello.maparserver.service;

import com.rawello.maparserver.model.Building;
import com.rawello.maparserver.model.BuildingDownloadStatistic;
import com.rawello.maparserver.repository.BuildingDownloadStatisticRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class BuildingDownloadStatisticService {
    private static final Logger logger = LoggerFactory.getLogger(BuildingDownloadStatisticService.class);
    private final BuildingDownloadStatisticRepository statisticRepository;

    public BuildingDownloadStatisticService(BuildingDownloadStatisticRepository statisticRepository) {
        this.statisticRepository = statisticRepository;
    }

    public void incrementDownloadCount(Building building) {
        LocalDate today = LocalDate.now();
        Long buildingId = building.getId();
        Optional<BuildingDownloadStatistic> statOpt = statisticRepository.findByBuildingIdAndDate(buildingId, today);
        BuildingDownloadStatistic stat = statOpt.orElseGet(() -> new BuildingDownloadStatistic(buildingId, today, 0));
        int before = stat.getDownloadCount();
        stat.setDownloadCount(before + 1);
        statisticRepository.save(stat);
        logger.info("[DOWNLOAD_STAT] buildingId={}, date={}, before={}, after={}", buildingId, today, before,
                stat.getDownloadCount());
    }

    public List<BuildingDownloadStatistic> getStatisticsForDay(Long buildingId, LocalDate date) {
        return statisticRepository.findByBuildingIdAndDate(buildingId, date)
                .map(List::of)
                .orElse(List.of());
    }

    public List<BuildingDownloadStatistic> getStatisticsForWeek(Long buildingId, LocalDate dateInWeek) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        LocalDate startOfWeek = dateInWeek.with(weekFields.dayOfWeek(), 1);
        LocalDate endOfWeek = dateInWeek.with(weekFields.dayOfWeek(), 7);
        return statisticRepository.findByBuildingIdAndDateRange(buildingId, startOfWeek, endOfWeek);
    }

    public List<BuildingDownloadStatistic> getStatisticsForMonth(Long buildingId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return statisticRepository.findByBuildingIdAndDateRange(buildingId, start, end);
    }
}
