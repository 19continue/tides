package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(title = "CinemaManageVo", description = "Cinema manage")
public class CinemaManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long areaId;

    private String cityName;

    private String districtName;

    private String businessArea;

    private String cinemaName;

    private String brandName;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String phone;

    private String featureTags;

    private String trafficInfo;

    private String parkingInfo;

    private String openingHours;

    private String announcement;

    private String coverUrl;

    private Integer status;

    private Date createTime;

    private Date editTime;
}
