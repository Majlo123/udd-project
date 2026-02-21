package com.udd.forensic.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoSearchRequestDTO {

    @NotNull(message = "Geografska širina (latitude) je obavezna")
    private Double latitude;

    @NotNull(message = "Geografska dužina (longitude) je obavezna")
    private Double longitude;

    @NotNull(message = "Radijus je obavezan")
    @Positive(message = "Radijus mora biti pozitivna vrednost")
    private Double radiusKm;
}
