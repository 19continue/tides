package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "VenueManageDto", description = "Venue manage query")
public class VenueManageDto extends BasePageDto {

    private Long venueId;

    private Long areaId;

    private String venueName;

    private Integer venueType;

    private Integer status;
}
