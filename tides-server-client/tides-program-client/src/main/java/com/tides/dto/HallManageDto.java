package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "HallManageDto", description = "Hall manage query")
public class HallManageDto extends BasePageDto {

    @Schema(name = "hallId", type = "Long", description = "hall id")
    private Long hallId;

    @Schema(name = "cinemaId", type = "Long", description = "cinema id")
    private Long cinemaId;

    @Schema(name = "hallName", type = "String", description = "hall name")
    private String hallName;

    @Schema(name = "status", type = "Integer", description = "status")
    private Integer status;
}
