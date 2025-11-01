package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @description: 节目简单信息 vo
 * @author: 19continue
 **/
@Data
@Schema(title="ProgramSimpleInfoVo", description ="节目简单信息")
public class ProgramSimpleInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    
    @Schema(name ="id", type ="Long", description ="主键id")
    private Long programId;
    
    @Schema(name ="areaId", type ="Long", description ="地区id")
    private Long areaId;
    
    @Schema(name ="areaIdName", type ="String", description ="地区名字")
    private String areaIdName;

    @Schema(name ="title", type ="String", description ="节目标题")
    private String title;

    @Schema(name ="place", type ="String", description ="场馆/影院")
    private String place;

    @Schema(name ="showTime", type ="Date", description ="演出/放映时间")
    private Date showTime;

    @Schema(name ="showWeekTime", type ="String", description ="演出/放映时间所在的星期")
    private String showWeekTime;
}
