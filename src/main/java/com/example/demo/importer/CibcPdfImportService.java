package com.example.demo.importer;

import com.example.demo.transaction.TransactionEntity;
import com.example.demo.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.StatementImportEntity;
import com.example.demo.repository.StatementImportRepository;
import java.time.LocalDateTime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CibcPdfImportService {

  private final TransactionRepository repo;
  private final StatementImportRepository statementImportRepository;

  public CibcPdfImportService(
      TransactionRepository repo,
      StatementImportRepository statementImportRepository
  ){
    this.repo = repo;
    this.statementImportRepository = statementImportRepository;
  }   

  // Normal one-line row:
  // Nov 25 Nov 26 Columbia Sportswear CA London ON Retail and Grocery 119.03
  private static final Pattern TX_ROW = Pattern.compile(
      "^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2})\\s+" +
      "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2})\\s+" +
      "(.+?)\\s+" +
      "(-?[0-9,]+\\.\\d{2})\\s*$"
  );

  // First line of foreign currency row:
  // Dec 17 Dec 18 OPENAI *CHATGPT SUBSCR OPENAI.COM CA
  private static final Pattern FX_ROW_START = Pattern.compile(
      "^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2})\\s+" +
      "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2})\\s+" +
      "(.+)$"
  );

  // Second line of foreign currency row:
  // 21.00 USD @ 1.412857143**
  private static final Pattern FX_RATE_LINE = Pattern.compile(
      "^[0-9,]+\\.\\d{2}\\s+[A-Z]{3}\\s+@\\s+[0-9.]+\\*{0,2}\\s*$"
  );

  // Third line of foreign currency row:
  // Foreign Currency Transactions 29.67
  private static final Pattern FX_AMOUNT_LINE = Pattern.compile(
      "^(.+?)\\s+(-?[0-9,]+\\.\\d{2})\\s*$"
  );

  private static final Set<String> KNOWN_CATEGORIES = Set.of(
      "Retail and Grocery",
      "Personal and Household Expenses",
      "Professional and Financial Services",
      "Hotel, Entertainment and Recreation",
      "Foreign Currency Transactions"
  );

  public record ImportResult(int rowsInserted, int rowsSkipped) {}

  public ImportResult importStatement(MultipartFile file) throws Exception {
    byte[] fileBytes = file.getBytes();
    String fileHash = sha256Bytes(fileBytes);

    if (statementImportRepository.findByFileHash(fileHash).isPresent()) {
      return new ImportResult(0, 0);
    }

    String text = PdfTextExtractor.extractAllText(new java.io.ByteArrayInputStream(fileBytes));
    List<String> rawLines = Arrays.asList(text.split("\\R"));

    int statementYear = guessStatementYear(text);

    LocalDate statementStartDate = guessStatementStartDate(text);
    LocalDate statementEndDate = guessStatementEndDate(text);

    StatementImportEntity statementImport = new StatementImportEntity();
    statementImport.setFilename(file.getOriginalFilename());
    statementImport.setSource("CIBC");
    statementImport.setStatementStartDate(statementStartDate);
    statementImport.setStatementEndDate(statementEndDate);
    statementImport.setUploadedAt(LocalDateTime.now());
    statementImport.setRowsInserted(0);
    statementImport.setRowsSkipped(0);
    statementImport.setFileHash(fileHash);

    statementImport = statementImportRepository.save(statementImport);

    String currentCardRef = null;
    boolean inChargesSection = false;

    int inserted = 0;
    int skipped = 0;

    for (int i = 0; i < rawLines.size(); i++) {
      String line = cleanLine(rawLines.get(i));
      if (line.isEmpty()) continue;

      // Start parsing only inside charges section
      if (line.startsWith("Your new charges and credits")) {
        inChargesSection = true;
        continue;
      }

      if (!inChargesSection) {
        continue;
      }

      // Skip obvious junk/header lines inside section
      if (shouldSkipLine(line)) {
        continue;
      }

      // Detect card section
      if (line.startsWith("Card number")) {
        String[] parts = line.split("\\s+");
        currentCardRef = parts[parts.length - 1];
        continue;
      }

      // Stop if we somehow drift into unrelated footer text
      if (line.startsWith("Total payments")
          || line.startsWith("Total interest")
          || line.startsWith("Your installment summary")
          || line.startsWith("Information about your CIBC")) {
        continue;
      }

      // ---------- 1) Try normal one-line transaction ----------
      Matcher normal = TX_ROW.matcher(line);
      if (normal.matches()) {
        ParsedRow row = buildNormalRow(statementYear, currentCardRef, normal);
        if (saveIfNew(row, statementImport)) inserted++;
        else skipped++;
        continue;
      }

      // ---------- 2) Try foreign-currency multiline transaction ----------
      Matcher fxStart = FX_ROW_START.matcher(line);
      if (fxStart.matches() && i + 2 < rawLines.size()) {
        String line2 = cleanLine(rawLines.get(i + 1));
        String line3 = cleanLine(rawLines.get(i + 2));

        if (FX_RATE_LINE.matcher(line2).matches()) {
          Matcher fxAmount = FX_AMOUNT_LINE.matcher(line3);
          if (fxAmount.matches()) {
            String bankCategory = fxAmount.group(1).trim();
            String amountStr = fxAmount.group(2).replace(",", "");
            long amountCents = Math.round(Double.parseDouble(amountStr) * 100.0);

            ParsedRow row = new ParsedRow();
            row.accountId = 1L;
            row.transDate = parseDate(statementYear, fxStart.group(1), fxStart.group(2));
            row.postDate = parseDate(statementYear, fxStart.group(3), fxStart.group(4));
            row.description = fxStart.group(5).trim();
            row.bankCategory = bankCategory;
            row.cardRef = currentCardRef;
            row.amountCents = amountCents;
            row.currency = "CAD";
            row.source = "CIBC";

            if (saveIfNew(row, statementImport)) inserted++;
            else skipped++;

            i += 2; // consume the next 2 lines
            continue;
          }
        }
      }
    }

    statementImport.setRowsInserted(inserted);
    statementImport.setRowsSkipped(skipped);
    statementImportRepository.save(statementImport);

    return new ImportResult(inserted, skipped);
  }

  private ParsedRow buildNormalRow(int year, String currentCardRef, Matcher m) {
    LocalDate transDate = parseDate(year, m.group(1), m.group(2));
    LocalDate postDate = parseDate(year, m.group(3), m.group(4));

    String middle = m.group(5).trim();
    String amountStr = m.group(6).replace(",", "");
    long amountCents = Math.round(Double.parseDouble(amountStr) * 100.0);

    String bankCategory = null;
    String description = middle;

    for (String cat : KNOWN_CATEGORIES) {
      if (middle.endsWith(cat)) {
        bankCategory = cat;
        description = middle.substring(0, middle.length() - cat.length()).trim();
        break;
      }
    }

    ParsedRow row = new ParsedRow();
    row.accountId = 1L;
    row.transDate = transDate;
    row.postDate = postDate;
    row.description = description;
    row.bankCategory = bankCategory;
    row.cardRef = currentCardRef;
    row.amountCents = amountCents;
    row.currency = "CAD";
    row.source = "CIBC";
    return row;
  }

  private boolean saveIfNew(ParsedRow row, StatementImportEntity statementImport) throws Exception {
    String importHash = sha256(
        row.accountId + "|" +
        row.transDate + "|" +
        row.postDate + "|" +
        normalize(row.description) + "|" +
        row.amountCents + "|" +
        Objects.toString(row.cardRef, "")
    );

    if (repo.existsByImportHash(importHash)) {
      return false;
    }

    TransactionEntity tx = new TransactionEntity();
    tx.setAccountId(row.accountId);
    tx.setTransDate(row.transDate);
    tx.setPostedDate(row.postDate);
    tx.setDescription(row.description);
    tx.setBankCategory(row.bankCategory);
    tx.setCardRef(row.cardRef);
    tx.setAmountCents(row.amountCents);
    tx.setCurrency(row.currency);
    tx.setSource(row.source);
    tx.setImportHash(importHash);
    tx.setStatementImport(statementImport);

    repo.save(tx);
    return true;
  }

  private static boolean shouldSkipLine(String line) {
    return line.equals("Trans")
        || line.equals("date")
        || line.equals("Post")
        || line.equals("Description")
        || line.equals("Spend Categories")
        || line.equals("Amount($)")
        || line.startsWith("Prepared for:")
        || line.startsWith("Page ")
        || line.startsWith("Q Identifies")
        || line.startsWith("*0302210000*")
        || line.startsWith("** Denotes transaction in foreign currency");
  }

  private static String cleanLine(String s) {
    return s == null ? "" : s.trim().replaceAll("\\s+", " ");
  }

  private static int guessStatementYear(String fullText) {
    Pattern p = Pattern.compile(
        "Transactions from\\s+[A-Za-z]+\\s+\\d{1,2}\\s+to\\s+[A-Za-z]+\\s+\\d{1,2},\\s+(20\\d{2})"
    );
    Matcher m = p.matcher(fullText);
    if (m.find()) {
        return Integer.parseInt(m.group(1));
    }
    return Year.now().getValue();
  }

  private static LocalDate guessStatementStartDate(String fullText) {
    Pattern p = Pattern.compile(
        "Transactions from\\s+([A-Za-z]+)\\s+(\\d{1,2})\\s+to\\s+([A-Za-z]+)\\s+(\\d{1,2}),\\s+(20\\d{2})"
    );

    Matcher m = p.matcher(fullText);
    if (m.find()) {
      int year = Integer.parseInt(m.group(5));
      return parseDate(year, m.group(1).substring(0, 3), m.group(2));
    }

    return null;
  }

  private static LocalDate guessStatementEndDate(String fullText) {
    Pattern p = Pattern.compile(
        "Transactions from\\s+([A-Za-z]+)\\s+(\\d{1,2})\\s+to\\s+([A-Za-z]+)\\s+(\\d{1,2}),\\s+(20\\d{2})"
    );

    Matcher m = p.matcher(fullText);
    if (m.find()) {
      int year = Integer.parseInt(m.group(5));
      return parseDate(year, m.group(3).substring(0, 3), m.group(4));
    }

    return null;
  }

  private static LocalDate parseDate(int year, String mon, String day) {
    int month = switch (mon) {
      case "Jan" -> 1;
      case "Feb" -> 2;
      case "Mar" -> 3;
      case "Apr" -> 4;
      case "May" -> 5;
      case "Jun" -> 6;
      case "Jul" -> 7;
      case "Aug" -> 8;
      case "Sep" -> 9;
      case "Oct" -> 10;
      case "Nov" -> 11;
      case "Dec" -> 12;
      default -> throw new IllegalArgumentException("Bad month: " + mon);
    };

    return LocalDate.of(year, month, Integer.parseInt(day));
  }

  private static String normalize(String s) {
    return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  private static String sha256(String input) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : hash) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private static String sha256Bytes(byte[] input) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hash = md.digest(input);
    StringBuilder sb = new StringBuilder();
    for (byte b : hash) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private static class ParsedRow {
    long accountId;
    LocalDate transDate;
    LocalDate postDate;
    String description;
    String bankCategory;
    String cardRef;
    long amountCents;
    String currency;
    String source;
  }
}