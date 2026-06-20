package com.demo.repository;

import com.demo.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Integer> {
    Optional<Driver> findByPhoneNo(String phoneNo);
    boolean existsByPhoneNo(String phoneNo);
}