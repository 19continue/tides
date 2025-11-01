package com.tides.dto;

import lombok.Data;

/**
 * @description: elasticsearch数据参数
 * @author: 19continue
 **/
@Data
public class EsDataCreateDto {
    
    /**
     * 字段名
     * */
    private String paramName;
    /**
     * 字段值
     * */
    private Object paramValue;
}
