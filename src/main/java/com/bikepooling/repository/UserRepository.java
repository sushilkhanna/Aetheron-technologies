package com.bikepooling.repository;

import com.bikepooling.entity.User;
import com.bikepooling.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {
    public boolean existsByPhone(String phone);
    public Optional<User> findByPhone(String phone);
    boolean existsByAadhaarNumber(String aadhaarNumber);
    Optional<User> findByAadhaarNumber(String aadhaarNumber);
    boolean existsByDlNumber(String dlNumber);
    Optional<User> findByDlNumber(String dlNumber);
    Optional<User> findByEmail(String email);

    List<User> findByRoleAndActiveTrue(Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();
}