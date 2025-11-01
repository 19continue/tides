package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @description: 账户下订单数量 vo
 * @author: 19continue
 **/
@Data
@Schema(title="AccountOrderCountVo", description ="账户下订单数量")
public class AccountOrderCountVo {
    
    @Schema(name ="count", type ="Integer", description ="账户下的订单数量")
    private Integer count;
}
