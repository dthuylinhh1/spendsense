package com.example.demo.transaction;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    List<TransactionEntity> findAllByOrderByPostedDateDescIdDesc();

    List<TransactionEntity> findByStatementImport_Id(Long statementImportId);

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
    List<TransactionEntity> findByStatementImport_IdAndCardRefOrderByPostedDateDescIdDesc(
        Long statementImportId,
        String cardRef
        );
    boolean existsByImportHash(String importHash);

    @Query("""
        select coalesce(sum(t.amountCents), 0)
        from TransactionEntity t
        where t.statementImport.id = :statementImportId
        """)
        Long getTotalSpendingCentsByStatementImport(
                @Param("statementImportId") Long statementImportId
        );

        @Query("""
        select count(t)
        from TransactionEntity t
        where t.statementImport.id = :statementImportId
        """)
        long countTransactionsByStatementImport(
                @Param("statementImportId") Long statementImportId
        );

        @Query("""
        select t.cardRef, coalesce(sum(t.amountCents), 0)
        from TransactionEntity t
        where t.statementImport.id = :statementImportId
        group by t.cardRef
        order by coalesce(sum(t.amountCents), 0) desc
        """)
        List<Object[]> getSpendingByCardForStatementImport(
                @Param("statementImportId") Long statementImportId
        );

        @Query("""
        select t.bankCategory, coalesce(sum(t.amountCents), 0)
        from TransactionEntity t
        where t.statementImport.id = :statementImportId
        group by t.bankCategory
        order by coalesce(sum(t.amountCents), 0) desc
        """)
        List<Object[]> getTopCategoriesForStatementImport(
                @Param("statementImportId") Long statementImportId
        );

        @Query("""
        select t.bankCategory, coalesce(sum(t.amountCents), 0)
        from TransactionEntity t
        where t.statementImport.id = :statementImportId
        group by t.bankCategory
        order by coalesce(sum(t.amountCents), 0) desc
        """)
        List<Object[]> getCategoryTotalsByStatementImport(
                @Param("statementImportId") Long statementImportId
        );
}