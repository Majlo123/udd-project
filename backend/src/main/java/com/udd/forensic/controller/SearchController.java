package com.udd.forensic.controller;

import com.udd.forensic.dto.GeoSearchRequestDTO;
import com.udd.forensic.dto.SearchRequestDTO;
import com.udd.forensic.model.ForensicReport;
import com.udd.forensic.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for all search operations:
 * - Simple text search
 * - Advanced boolean search (Stack Machine)
 * - Geo-spatial search
 * - Hybrid (text + geo) search
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * POST /api/search
     * Main search endpoint supporting simple, advanced, and hybrid (text + geo) search.
     *
     * Request body example:
     * {
     *   "query": "(malware:WannaCry OR description:\"enkripcija fajlova\") AND NOT classification:Spyware",
     *   "advancedSearch": true,
     *   "latitude": 44.7866,
     *   "longitude": 20.4489,
     *   "radiusKm": 100.0
     * }
     */
    @PostMapping
    public ResponseEntity<List<ForensicReport>> search(@RequestBody SearchRequestDTO dto) {
        log.info("POST /api/search - query='{}', advanced={}, hasGeo={}",
                dto.getQuery(), dto.isAdvancedSearch(),
                dto.getLatitude() != null && dto.getLongitude() != null);

        List<ForensicReport> results = searchService.search(dto);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/search/simple?q=...
     * Simple text search across all fields with fuzzy matching.
     */
    @GetMapping("/simple")
    public ResponseEntity<List<ForensicReport>> simpleSearch(@RequestParam("q") String query) {
        log.info("GET /api/search/simple - query='{}'", query);

        SearchRequestDTO dto = new SearchRequestDTO();
        dto.setQuery(query);
        dto.setAdvancedSearch(false);

        List<ForensicReport> results = searchService.search(dto);
        return ResponseEntity.ok(results);
    }

    /**
     * POST /api/search/geo
     * Geo-only search: finds reports within a radius of a given point.
     */
    @PostMapping("/geo")
    public ResponseEntity<List<ForensicReport>> geoSearch(
            @Valid @RequestBody GeoSearchRequestDTO dto) {
        log.info("POST /api/search/geo - lat={}, lon={}, radius={}km",
                dto.getLatitude(), dto.getLongitude(), dto.getRadiusKm());

        List<ForensicReport> results = searchService.searchByLocation(
                dto.getLatitude(), dto.getLongitude(), dto.getRadiusKm());
        return ResponseEntity.ok(results);
    }
}
