package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "CinemaManageDto", description = "Cinema manage query")
public class CinemaManageDto extends BasePageDto {

    @Schema(name = "cinemaId", type = "Long", description = "cinema id")
    private Long cinemaId;

    @Schema(name = "areaId", type = "Long", description = "area id")
    private Long areaId;

    @Schema(name = "cityName", type = "String", description = "city name")
    private String cityName;

    @Schema(name = "districtName", type = "String", description = "district name")
    private String districtName;

    @Schema(name = "businessArea", type = "String", description = "business area")
    private String businessArea;

    @Schema(name = "brandName", type = "String", description = "brand name")
    private String brandName;

    @Schema(name = "cinemaName", type = "String", description = "cinema name")
    private String cinemaName;

    @Schema(name = "status", type = "Integer", description = "status")
    private Integer status;
}
