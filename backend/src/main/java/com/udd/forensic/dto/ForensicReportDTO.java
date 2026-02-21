package com.udd.forensic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForensicReportDTO {

    @NotBlank(message = "Ime forenzičara je obavezno")
    private String forensicInvestigator;

    @NotBlank(message = "Organizacija je obavezna")
    private String organization;

    private String malwareName;

    private String description;

    @NotBlank(message = "Klasifikacija je obavezna")
    private String classification;

    private String fileHash;

    @NotBlank(message = "Grad je obavezan")
    private String city;

    private Double latitude;

    private Double longitude;
}
