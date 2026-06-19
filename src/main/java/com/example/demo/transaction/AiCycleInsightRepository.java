package com.example.demo.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiCycleInsightRepository extends JpaRepository<AiCycleInsightEntity, Long> {

    Optional<AiCycleInsightEntity> findByCycleAIdAndCycleBIdAndModel(
            Long cycleAId,
            Long cycleBId,
            String model
    );

    boolean existsByCycleAIdAndCycleBIdAndModel(
            Long cycleAId,
            Long cycleBId,
            String model
    );
}
