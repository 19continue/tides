package com.tides.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @description: elasticsearch GeoPoint
 * @author: 19continue
 **/
@Data
public class EsGeoPointDto {
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
