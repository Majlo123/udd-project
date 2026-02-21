package com.udd.forensic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDTO {

    /** Tekst pretrage (npr. "WannaCry AND classification:Ransomware") */
    private String query;

    /** Da li koristiti naprednu boolean pretragu (Stack Machine) */
    private boolean advancedSearch;

    /** Geo-lokacijski parametri (opcioni) */
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
}
