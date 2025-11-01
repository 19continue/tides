package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @description: 电影场次座位相关信息 vo
 */
@Data
@Schema(title = "MovieSeatRelateInfoVo", description = "电影场次座位相关信息")
public class MovieSeatRelateInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "screeningId", type = "Long", description = "放映场次id")
    private Long screeningId;

    @Schema(name = "programId", type = "Long", description = "关联节目id")
    private Long programId;

    @Schema(name = "movieId", type = "Long", description = "影片id")
    private Long movieId;

    @Schema(name = "cinemaId", type = "Long", description = "影院id")
    private Long cinemaId;

    @Schema(name = "cinemaName", type = "String", description = "影院名称")
    private String cinemaName;

    @Schema(name = "hallId", type = "Long", description = "影厅id")
    private Long hallId;

    @Schema(name = "hallName", type = "String", description = "影厅名称")
    private String hallName;

    @Schema(name = "hallType", type = "String", description = "影厅类型")
    private String hallType;

    @Schema(name = "showTime", type = "Date", description = "放映开始时间")
    private Date showTime;

    @Schema(name = "showWeekTime", type = "String", description = "放映星期")
    private String showWeekTime;

    @Schema(name = "priceList", type = "List<String>", description = "价格集合")
    private List<String> priceList;

    @Schema(name = "seatVoMap", type = "Map<String,List<SeatVo>>", description = "座位集合")
    private Map<String, List<SeatVo>> seatVoMap;
}
