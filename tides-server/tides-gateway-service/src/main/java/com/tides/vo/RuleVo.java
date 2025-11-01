package com.tides.vo;

import lombok.Data;

/**
 * @description: 普通对象 返回vo
 * @author: 19continue
 **/
@Data
public class RuleVo {
    
    private String id;
    
    private Integer statTime;
    
    private Integer statTimeType;
    
    private Integer threshold;
    
    private Integer effectiveTime;
    
    private Integer effectiveTimeType;
    
    private String limitApi;
    
    private String message;
    
    private Integer status;
}
