package com.udd.forensic.service;

import com.udd.forensic.model.ForensicReport;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for logging statistics about new forensic reports.
 * Logs are written in a structured format that Logstash can parse with Grok filters.
 *
 * Format: [STATS] :: {grad} :: {forenzicar} :: {tip_pretnje}
 * These logs are picked up by Logstash, parsed, and sent to a separate
 * Elasticsearch index (forensic-stats-YYYY.MM.dd) for Kibana dashboards.
 */
@Slf4j
@Service
public class StatsService {

    private static final Logger statsLogger = LoggerFactory.getLogger("StatsLogger");

    /**
     * Logs a new report entry for Logstash processing.
     *
     * @param report the saved forensic report
     */
    public void logNewReport(ForensicReport report) {
        String logEntry = String.format("[STATS] :: %s :: %s :: %s",
                sanitize(report.getCity()),
                sanitize(report.getForensicInvestigator()),
                sanitize(report.getClassification())
        );
        statsLogger.info(logEntry);
        log.debug("Stats logged: {}", logEntry);
    }

    /**
     * Sanitizes a string for safe inclusion in the log entry.
     * Removes '::' sequences and trims whitespace to prevent Grok parse issues.
     */
    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return value.replace("::", "").trim();
    }
}
