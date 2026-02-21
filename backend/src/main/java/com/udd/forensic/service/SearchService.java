package com.udd.forensic.service;

import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.udd.forensic.dto.SearchRequestDTO;
import com.udd.forensic.model.ForensicReport;
import com.udd.forensic.search.QueryParser;
import com.udd.forensic.search.StackMachine;
import com.udd.forensic.search.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core search service that orchestrates all search functionality:
 * - Simple text search (multi-match across all fields)
 * - Advanced boolean search (Stack Machine with Shunting-yard algorithm)
 * - Phrase search (MatchPhraseQuery)
 * - Fuzzy search (fuzziness: AUTO)
 * - Geo-spatial search (GeoDistanceQuery)
 * - Hybrid search (combining text + geo filters)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Main search method that handles all types of searches.
     *
     * @param dto search request parameters
     * @return list of matching forensic reports
     */
    public List<ForensicReport> search(SearchRequestDTO dto) {
        Query textQuery;

        if (dto.isAdvancedSearch() && dto.getQuery() != null && !dto.getQuery().isBlank()) {
            // Advanced boolean search using Stack Machine
            textQuery = buildAdvancedQuery(dto.getQuery());
        } else if (dto.getQuery() != null && !dto.getQuery().isBlank()) {
            // Simple multi-match search with fuzziness
            textQuery = buildSimpleQuery(dto.getQuery());
        } else {
            textQuery = Query.of(q -> q.matchAll(m -> m));
        }

        // Build final query (optionally adding geo filter)
        Query finalQuery = combineWithGeoFilter(textQuery, dto);

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(finalQuery)
                .build();

        SearchHits<ForensicReport> searchHits = elasticsearchOperations.search(
                nativeQuery, ForensicReport.class);

        List<ForensicReport> results = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        log.info("Search completed. Query: '{}', Advanced: {}, Results: {}",
                dto.getQuery(), dto.isAdvancedSearch(), results.size());

        return results;
    }

    /**
     * Geo-only search: finds reports within a radius of a point.
     */
    public List<ForensicReport> searchByLocation(double lat, double lon, double radiusKm) {
        Query geoQuery = buildGeoDistanceQuery(lat, lon, radiusKm);

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(geoQuery)
                .build();

        SearchHits<ForensicReport> searchHits = elasticsearchOperations.search(
                nativeQuery, ForensicReport.class);

        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    // ==================== Query Builders ====================

    /**
     * Builds an advanced boolean query using the Stack Machine.
     * Parses the user's query string (infix) -> tokenize -> convert to postfix -> build ES query.
     */
    private Query buildAdvancedQuery(String queryString) {
        log.debug("Building advanced query from: {}", queryString);

        List<Token> postfixTokens = QueryParser.parse(queryString);
        log.debug("Postfix tokens: {}", postfixTokens);

        return StackMachine.buildQuery(postfixTokens);
    }

    /**
     * Builds a simple multi-match query across all searchable fields with fuzzy matching.
     */
    private Query buildSimpleQuery(String queryString) {
        return MultiMatchQuery.of(mm -> mm
                .fields("content", "description", "forensicInvestigator",
                        "organization", "malwareName", "classification", "city")
                .query(queryString)
                .fuzziness("AUTO")
                .type(TextQueryType.BestFields)
        )._toQuery();
    }

    /**
     * Builds a geo-distance filter query.
     */
    private Query buildGeoDistanceQuery(double lat, double lon, double radiusKm) {
        return GeoDistanceQuery.of(g -> g
                .field("location")
                .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                .distance(radiusKm + "km")
                .distanceType(GeoDistanceType.Arc)
        )._toQuery();
    }

    /**
     * Combines a text query with an optional geo-distance filter using a BoolQuery.
     * If geo params are present, the text query goes in "must" and geo goes in "filter".
     */
    private Query combineWithGeoFilter(Query textQuery, SearchRequestDTO dto) {
        if (dto.getLatitude() != null && dto.getLongitude() != null && dto.getRadiusKm() != null) {
            Query geoFilter = buildGeoDistanceQuery(
                    dto.getLatitude(), dto.getLongitude(), dto.getRadiusKm());

            return BoolQuery.of(b -> b
                    .must(textQuery)
                    .filter(geoFilter)
            )._toQuery();
        }

        return textQuery;
    }
}
