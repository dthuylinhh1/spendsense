package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "statement_imports")
public class StatementImportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;

    private String source; // example: CIBC

    private LocalDate statementStartDate;

    private LocalDate statementEndDate;

    private LocalDateTime uploadedAt;

    private int rowsInserted;

    private int rowsSkipped;

    @Column(unique = true, nullable = false)
    private String fileHash;

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDate getStatementStartDate() {
        return statementStartDate;
    }

    public void setStatementStartDate(LocalDate statementStartDate) {
        this.statementStartDate = statementStartDate;
    }

    public LocalDate getStatementEndDate() {
        return statementEndDate;
    }

    public void setStatementEndDate(LocalDate statementEndDate) {
        this.statementEndDate = statementEndDate;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public int getRowsInserted() {
        return rowsInserted;
    }

    public void setRowsInserted(int rowsInserted) {
        this.rowsInserted = rowsInserted;
    }

    public int getRowsSkipped() {
        return rowsSkipped;
    }

    public void setRowsSkipped(int rowsSkipped) {
        this.rowsSkipped = rowsSkipped;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
}