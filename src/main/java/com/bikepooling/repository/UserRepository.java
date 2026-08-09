package com.bikepooling.repository;

import com.bikepooling.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    public boolean existsByPhone(String phone);
    public Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);
}
