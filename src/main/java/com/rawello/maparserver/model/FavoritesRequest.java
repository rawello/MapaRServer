package com.rawello.maparserver.model;

import java.util.List;

public class FavoritesRequest {
    private List<Long> favoriteBuildings;

    public List<Long> getFavoriteBuildings() {
        return favoriteBuildings;
    }

    public void setFavoriteBuildings(List<Long> favoriteBuildings) {
        this.favoriteBuildings = favoriteBuildings;
    }
}