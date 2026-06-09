package com.example.demo.importer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CibcPdfImportPageController {

    private final CibcPdfImportService service;

    public CibcPdfImportPageController(CibcPdfImportService service) {
        this.service = service;
    }

    @PostMapping("/transactions/import/cibc-pdf")
    public String importCibcPdfFromPage(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute(
                        "importError",
                        "Please choose a PDF statement before importing."
                );
                return "redirect:/transactions/view";
            }

            var result = service.importStatement(file);

            if (result.rowsInserted() > 0 && result.rowsSkipped() > 0) {
                redirectAttributes.addFlashAttribute(
                        "importSuccess",
                        "PDF uploaded successfully. "
                                + result.rowsInserted()
                                + " new transactions imported, "
                                + result.rowsSkipped()
                                + " duplicates skipped."
                );
            } else if (result.rowsInserted() > 0) {
                redirectAttributes.addFlashAttribute(
                        "importSuccess",
                        "PDF uploaded successfully. "
                                + result.rowsInserted()
                                + " transactions imported."
                );
            } else if (result.rowsSkipped() > 0) {
                redirectAttributes.addFlashAttribute(
                        "importWarning",
                        "This statement looks already uploaded. No new transactions imported. "
                                + result.rowsSkipped()
                                + " duplicates skipped."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "importWarning",
                        "PDF uploaded, but no transactions were found. Please check the statement format."
                );
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "importError",
                    "Import failed: " + e.getMessage()
            );
        }

        return "redirect:/transactions/view";
    }
}