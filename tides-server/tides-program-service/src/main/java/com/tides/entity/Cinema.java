package com.tides.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tides.data.BaseTableData;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description: 电影院实体
 */
@Data
@TableName("d_cinema")
public class Cinema extends BaseTableData implements Serializable {

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
}
