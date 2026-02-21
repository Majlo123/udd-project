package com.udd.forensic.repository;

import com.udd.forensic.model.ForensicReport;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForensicReportRepository extends ElasticsearchRepository<ForensicReport, String> {

    List<ForensicReport> findByContentContaining(String content);

    List<ForensicReport> findByClassification(String classification);

    List<ForensicReport> findByForensicInvestigatorContaining(String investigator);

    List<ForensicReport> findByMalwareNameContaining(String malwareName);

    List<ForensicReport> findByCity(String city);
}
