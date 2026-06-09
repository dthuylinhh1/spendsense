package com.example.demo.importer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
public class CibcPdfImportController {

  private final CibcPdfImportService service;

  public CibcPdfImportController(CibcPdfImportService service) {
    this.service = service;
  }

  @PostMapping("/cibc-pdf")
  public ResponseEntity<?> importCibcPdf(@RequestParam("file") MultipartFile file) throws Exception {
    var result = service.importStatement(file);
    return ResponseEntity.ok(result);
  }
}
