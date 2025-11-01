package com.tides.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @description: elasticsearch GeoPoint定位
 * @author: 19continue
 **/
@Data
public class EsGeoPointSortDto {
    /**
     * 字段名
     * */
    private String paramName;
    /**
     * 纬度值
     * */
    private BigDecimal latitude;
    /**
     * 经度值
     * */
    private BigDecimal longitude;
    
    
}
