package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "HallSeatPageManageDto", description = "Hall seat template page")
public class HallSeatPageManageDto extends BasePageDto {

    private Long cinemaId;

    private Long hallId;

    private Integer rowCode;

    private Integer colCode;

    private Integer seatStatus;
}
