package com.rawello.maparserver.repository;

import com.rawello.maparserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}