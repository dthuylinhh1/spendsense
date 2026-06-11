package com.example.demo.repository;

import com.example.demo.entity.StatementImportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StatementImportRepository extends JpaRepository<StatementImportEntity, Long> {

    Optional<StatementImportEntity> findByFileHash(String fileHash);

    List<StatementImportEntity> findAllByOrderByUploadedAtDesc();
}