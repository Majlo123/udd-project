package com.udd.forensic.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "forensic_reports")
@Setting(settingPath = "/elasticsearch/forensic-reports-settings.json")
public class ForensicReport {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, analyzer = "serbian")
    private String forensicInvestigator;

    @Field(type = FieldType.Text, store = true, analyzer = "serbian")
    private String organization;

    @Field(type = FieldType.Text, store = true)
    private String malwareName;

    @Field(type = FieldType.Text, store = true, analyzer = "serbian")
    private String description;

    @Field(type = FieldType.Keyword, store = true)
    private String classification;

    @Field(type = FieldType.Keyword, store = true)
    private String fileHash;

    @Field(type = FieldType.Text, store = true, analyzer = "serbian")
    private String content;

    @Field(type = FieldType.Keyword, store = true)
    private String minioPath;

    @Field(type = FieldType.Text, store = true, analyzer = "serbian")
    private String city;

    @GeoPointField
    private GeoPoint location;
}
