package com.rawello.maparserver.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "building_download_statistics")
public class BuildingDownloadStatistic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int downloadCount;

    public BuildingDownloadStatistic() {
    }

    public BuildingDownloadStatistic(Long buildingId, LocalDate date, int downloadCount) {
        this.buildingId = buildingId;
        this.date = date;
        this.downloadCount = downloadCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }
}
