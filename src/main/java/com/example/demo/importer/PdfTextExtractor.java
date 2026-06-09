package com.example.demo.importer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;

public class PdfTextExtractor {

  public static String extractAllText(InputStream in) throws Exception {
    // PDFBox 3.x uses Loader instead of PDDocument.load(InputStream)
    byte[] bytes = in.readAllBytes();
    try (PDDocument doc = Loader.loadPDF(bytes)) {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      return stripper.getText(doc);
    }
  }
}
