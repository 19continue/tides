package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @description: 电影放映场次 vo
 */
@Data
@Schema(title = "MovieScreeningVo", description = "电影放映场次")
public class MovieScreeningVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "id", type = "Long", description = "放映场次id")
    private Long id;

    @Schema(name = "movieId", type = "Long", description = "影片id")
    private Long movieId;

    @Schema(name = "programId", type = "Long", description = "关联节目id")
    private Long programId;

    @Schema(name = "cinemaId", type = "Long", description = "影院id")
    private Long cinemaId;

    @Schema(name = "cinemaName", type = "String", description = "影院名称")
    private String cinemaName;

    @Schema(name = "cinemaAddress", type = "String", description = "影院地址")
    private String cinemaAddress;

    @Schema(name = "hallId", type = "Long", description = "影厅id")
    private Long hallId;

    @Schema(name = "hallName", type = "String", description = "影厅名称")
    private String hallName;

    @Schema(name = "hallType", type = "String", description = "影厅类型")
    private String hallType;

    @Schema(name = "showTime", type = "Date", description = "放映开始时间")
    private Date showTime;

    @Schema(name = "showDayTime", type = "Date", description = "放映日期")
    private Date showDayTime;

    @Schema(name = "showWeekTime", type = "String", description = "放映星期")
    private String showWeekTime;

    @Schema(name = "endTime", type = "Date", description = "放映结束时间")
    private Date endTime;

    @Schema(name = "language", type = "String", description = "放映语言")
    private String language;

    @Schema(name = "version", type = "String", description = "放映版本")
    private String version;

    @Schema(name = "lowestPrice", type = "BigDecimal", description = "最低可售价格")
    private BigDecimal lowestPrice;

    @Schema(name = "stopSellTime", type = "Date", description = "停止售票时间")
    private Date stopSellTime;

    @Schema(name = "screeningStatus", type = "Integer", description = "场次状态：1可售 0下架")
    private Integer screeningStatus;
}
