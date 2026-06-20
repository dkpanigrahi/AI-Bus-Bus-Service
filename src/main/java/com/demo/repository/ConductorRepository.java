package com.demo.repository;

import com.demo.entity.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConductorRepository extends JpaRepository<Conductor, Integer> {
    Optional<Conductor> findByPhoneNo(String phoneNo);
    boolean existsByPhoneNo(String phoneNo);
}