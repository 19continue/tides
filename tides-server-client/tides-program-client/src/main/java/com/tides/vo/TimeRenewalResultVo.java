package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(title = "TimeRenewalResultVo", description = "Time renewal result")
public class TimeRenewalResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "renewalCount", type = "Integer", description = "renewed data count")
    private Integer renewalCount;

    @Schema(name = "programIdList", type = "List<Long>", description = "renewed program ids")
    private List<Long> programIdList;

    @Schema(name = "screeningIdList", type = "List<Long>", description = "renewed movie screening ids")
    private List<Long> screeningIdList;
}
