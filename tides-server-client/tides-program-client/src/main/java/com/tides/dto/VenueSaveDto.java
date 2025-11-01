package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(title = "VenueSaveDto", description = "Venue create or update")
public class VenueSaveDto {

    private Long id;

    private Long areaId;

    @NotBlank
    private String venueName;

    private Integer venueType;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String phone;

    private String trafficGuide;

    private String entryGuide;
}
