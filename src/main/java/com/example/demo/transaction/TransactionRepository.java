package com.example.demo.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    boolean existsByImportHash(String importHash);

    List<TransactionEntity> findAllByOrderByPostedDateDescIdDesc();

    List<TransactionEntity> findByCardRefOrderByPostedDateDescIdDesc(String cardRef);

    List<TransactionEntity> findByPostedDateBetweenOrderByPostedDateDescIdDesc(
            LocalDate start,
            LocalDate end
    );

    List<TransactionEntity> findByCardRefAndPostedDateBetweenOrderByPostedDateDescIdDesc(
            String cardRef,
            LocalDate start,
            LocalDate end
    );

    List<TransactionEntity> findByStatementImport_IdOrderByPostedDateDescIdDesc(Long statementImportId);
}