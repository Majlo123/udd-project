package com.udd.forensic.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for extracting text from documents using Apache Tika server.
 * Sends the document to the Tika REST API and receives extracted plain text.
 */
@Slf4j
@Service
public class TikaService {

    private final RestTemplate restTemplate;
    private final String tikaUrl;

    public TikaService(@Value("${tika.url}") String tikaUrl) {
        this.restTemplate = new RestTemplate();
        this.tikaUrl = tikaUrl;
    }

    /**
     * Extracts plain text content from a PDF or other document using Apache Tika.
     *
     * @param file the uploaded multipart document
     * @return extracted plain text content
     */
    public String extractText(MultipartFile file) {
        try {
            String endpoint = tikaUrl + "/tika";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set("Accept", "text/plain");

            HttpEntity<byte[]> request = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String extractedText = response.getBody().trim();
                log.info("Successfully extracted {} characters from document: {}",
                        extractedText.length(), file.getOriginalFilename());
                return extractedText;
            }

            log.warn("Tika returned empty or non-successful response for: {}", file.getOriginalFilename());
            return "";

        } catch (Exception e) {
            log.error("Failed to extract text via Tika for file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Greška prilikom ekstrakcije teksta iz dokumenta: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text from raw byte array.
     */
    public String extractText(byte[] fileContent, String contentType) {
        try {
            String endpoint = tikaUrl + "/tika";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    contentType != null ? contentType : "application/pdf"));
            headers.set("Accept", "text/plain");

            HttpEntity<byte[]> request = new HttpEntity<>(fileContent, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.PUT, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().trim();
            }
            return "";

        } catch (Exception e) {
            log.error("Failed to extract text from byte array", e);
            throw new RuntimeException("Greška prilikom ekstrakcije teksta: " + e.getMessage(), e);
        }
    }
}
