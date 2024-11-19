package com.rawello.maparserver.repository;

import com.rawello.maparserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.username = :username")
    User findByUsername(String username);

    boolean existsByUsername(String username);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserFavorites WHERE userId = :userId")
    void deleteAllFavoritesByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("INSERT INTO UserFavorites (userId, buildingId) VALUES (:userId, :buildingId)")
    void addFavoriteBuilding(@Param("userId") Long userId, @Param("buildingId") Long buildingId);

    @Query("SELECT uf.buildingId FROM UserFavorites uf WHERE uf.userId = :userId")
    List<Long> findFavoriteBuildingsByUserId(@Param("userId") Long userId);
}