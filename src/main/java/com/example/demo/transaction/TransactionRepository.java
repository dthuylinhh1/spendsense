package com.example.demo.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    boolean existsByImportHash(String importHash);

    List<TransactionEntity> findAllByOrderByPostedDateDescIdDesc();

    List<TransactionEntity> findByCardRefOrderByPostedDateDescIdDesc(String cardRef);

    List<TransactionEntity> findByPostedDateBetweenOrderByPostedDateDescIdDesc(
            LocalDate startDate,
            LocalDate endDate
    );

    List<TransactionEntity> findByCardRefAndPostedDateBetweenOrderByPostedDateDescIdDesc(
            String cardRef,
            LocalDate startDate,
            LocalDate endDate
    );
}