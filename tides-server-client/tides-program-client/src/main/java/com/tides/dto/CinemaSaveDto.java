package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(title = "CinemaSaveDto", description = "Cinema create or update")
public class CinemaSaveDto {

    @Schema(name = "id", type = "Long", description = "cinema id")
    private Long id;

    @Schema(name = "areaId", type = "Long", description = "area id")
    @NotNull
    private Long areaId;

    @Schema(name = "cityName", type = "String", description = "city name")
    private String cityName;

    @Schema(name = "districtName", type = "String", description = "district name")
    private String districtName;

    @Schema(name = "businessArea", type = "String", description = "business area")
    private String businessArea;

    @Schema(name = "cinemaName", type = "String", description = "cinema name")
    @NotBlank
    private String cinemaName;

    @Schema(name = "brandName", type = "String", description = "brand name")
    private String brandName;

    @Schema(name = "address", type = "String", description = "address")
    private String address;

    @Schema(name = "longitude", type = "BigDecimal", description = "longitude")
    private BigDecimal longitude;

    @Schema(name = "latitude", type = "BigDecimal", description = "latitude")
    private BigDecimal latitude;

    @Schema(name = "phone", type = "String", description = "phone")
    private String phone;

    @Schema(name = "featureTags", type = "String", description = "feature tags")
    private String featureTags;

    @Schema(name = "trafficInfo", type = "String", description = "traffic info")
    private String trafficInfo;

    @Schema(name = "parkingInfo", type = "String", description = "parking info")
    private String parkingInfo;

    @Schema(name = "openingHours", type = "String", description = "opening hours")
    private String openingHours;

    @Schema(name = "announcement", type = "String", description = "announcement")
    private String announcement;

    @Schema(name = "coverUrl", type = "String", description = "cover url")
    private String coverUrl;
}
