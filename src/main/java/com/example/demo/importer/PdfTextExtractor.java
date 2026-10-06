package com.example.demo.importer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class PdfTextExtractor {

  public static String extractAllText(File pdfFile) throws Exception {
    try (PDDocument doc = Loader.loadPDF(pdfFile)) {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      return stripper.getText(doc);
    }
  }
}
