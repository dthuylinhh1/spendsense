package com.example.demo.transaction;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

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

    List<TransactionEntity> findByStatementImport_IdOrderByPostedDateDescIdDesc(Long statementImportId);

    boolean existsByImportHash(String importHash);

    @Query("""
        select coalesce(sum(t.amountCents), 0)
        from TransactionEntity t
        where t.postedDate between :startDate and :endDate
    """)
    Long getTotalSpendingCentsByCycle(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        select count(t)
        from TransactionEntity t
        where t.postedDate between :startDate and :endDate
    """)
    long countTransactionsByCycle(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        select t.cardRef, coalesce(sum(t.amountCents), 0)
        from TransactionEntity t
        where t.postedDate between :startDate and :endDate
        group by t.cardRef
        order by coalesce(sum(t.amountCents), 0) desc
    """)
    List<Object[]> getSpendingByCardForCycle(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        select t.bankCategory, coalesce(sum(t.amountCents), 0)
        from TransactionEntity t
        where t.postedDate between :startDate and :endDate
        group by t.bankCategory
        order by coalesce(sum(t.amountCents), 0) desc
    """)
    List<Object[]> getTopCategoriesForCycle(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}