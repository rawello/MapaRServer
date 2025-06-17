package com.rawello.maparserver.repository;

import com.rawello.maparserver.model.BuildingDownloadStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BuildingDownloadStatisticRepository extends JpaRepository<BuildingDownloadStatistic, Long> {
        Optional<BuildingDownloadStatistic> findByBuildingIdAndDate(Long buildingId, LocalDate date);

        @Query("SELECT s FROM BuildingDownloadStatistic s WHERE s.buildingId = :buildingId AND s.date BETWEEN :startDate AND :endDate")
        List<BuildingDownloadStatistic> findByBuildingIdAndDateRange(@Param("buildingId") Long buildingId,
                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
