package com.udd.forensic.controller;

import com.udd.forensic.dto.ForensicReportDTO;
import com.udd.forensic.model.ForensicReport;
import com.udd.forensic.service.ForensicReportService;
import com.udd.forensic.service.MinioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * REST Controller for managing forensic reports.
 * Handles CRUD operations and PDF upload/download.
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ForensicReportController {

    private final ForensicReportService reportService;
    private final MinioService minioService;

    /**
     * POST /api/reports
     * Upload a new forensic report with metadata.
     * PDF is optional – if not provided, the system auto-generates one from the form data.
     *
     * @param dto     report metadata (JSON part)
     * @param pdfFile the PDF document (optional file part)
     * @return the created ForensicReport
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ForensicReport> createReport(
            @Valid @RequestPart("metadata") ForensicReportDTO dto,
            @RequestPart(value = "file", required = false) MultipartFile pdfFile) {

        log.info("POST /api/reports - Creating report for investigator: {} (pdf={})",
                dto.getForensicInvestigator(),
                pdfFile != null && !pdfFile.isEmpty() ? "uploaded" : "auto-generate");

        ForensicReport created = reportService.createReport(dto, pdfFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/reports
     * Retrieve all forensic reports.
     */
    @GetMapping
    public ResponseEntity<List<ForensicReport>> getAllReports() {
        List<ForensicReport> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * GET /api/reports/{id}
     * Retrieve a specific forensic report by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ForensicReport> getReport(@PathVariable String id) {
        return reportService.getReportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/reports/{id}
     * Delete a forensic report.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable String id) {
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/reports/{id}/download
     * Download the original PDF from MinIO.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadPdf(@PathVariable String id) {
        return reportService.getReportById(id)
                .map(report -> {
                    InputStream inputStream = minioService.downloadFile(report.getMinioPath());
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"report-" + id + ".pdf\"")
                            .body(new InputStreamResource(inputStream));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
