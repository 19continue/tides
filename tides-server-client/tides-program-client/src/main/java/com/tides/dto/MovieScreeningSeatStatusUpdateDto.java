package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(title = "MovieScreeningSeatStatusUpdateDto", description = "Movie screening seat status update")
public class MovieScreeningSeatStatusUpdateDto {

    @Schema(name = "screeningId", type = "Long", description = "screening id")
    @NotNull
    private Long screeningId;

    @Schema(name = "seatIdList", type = "List<Long>", description = "seat id list")
    @NotEmpty
    private List<Long> seatIdList;

    @Schema(name = "sellStatus", type = "Integer", description = "sell status")
    @NotNull
    private Integer sellStatus;

    @Schema(name = "bizNo", type = "String", description = "business number")
    private String bizNo;

    @Schema(name = "operatorId", type = "Long", description = "operator id")
    private Long operatorId;

    @Schema(name = "remark", type = "String", description = "remark")
    private String remark;
}
