package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description: 座位 vo
 * @author: 19continue
 **/
@Data
@Schema(title="SeatVo", description ="座位")
public class SeatVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    
    
    @Schema(name ="id", type ="Long", description ="座位id")
    private Long id;
    
    @Schema(name ="programId", type ="Long", description ="节目表id")
    private Long programId;
    
    @Schema(name ="ticketCategoryId", type ="Long", description ="节目票档id")
    private Long ticketCategoryId;
    
    @Schema(name ="rowCode", type ="Integer", description ="排号")
    private Integer rowCode;
  
    @Schema(name ="colCode", type ="Integer", description ="列号")
    private Integer colCode;

    @Schema(name ="seatNo", type ="String", description ="座位号展示值")
    private String seatNo;

    @Schema(name ="zoneName", type ="String", description ="座位分区")
    private String zoneName;

    @Schema(name ="priceLevel", type ="String", description ="价格区或座位等级")
    private String priceLevel;
    
    @Schema(name ="seatType", type ="Integer", description ="座位类型")
    private Integer seatType;
    
    @Schema(name ="seatTypeName", type ="String", description ="座位类型名")
    private String seatTypeName;
    
    @Schema(name ="price", type ="BigDecimal", description ="座位价格")
    private BigDecimal price;
    
    @Schema(name ="sellStatus", type ="Integer", description ="1未售卖 2锁定 3已售卖")
    private Integer sellStatus;

    @Schema(name ="aisleFlag", type ="Integer", description ="是否过道/空位 1是 0否")
    private Integer aisleFlag;

    @Schema(name ="coupleFlag", type ="Integer", description ="是否情侣座 1是 0否")
    private Integer coupleFlag;

    @Schema(name ="accessibleFlag", type ="Integer", description ="是否无障碍座 1是 0否")
    private Integer accessibleFlag;

    @Schema(name ="vipFlag", type ="Integer", description ="是否VIP座 1是 0否")
    private Integer vipFlag;

    @Schema(name ="repairFlag", type ="Integer", description ="是否维修座 1是 0否")
    private Integer repairFlag;

    @Schema(name ="sellableFlag", type ="Integer", description ="是否可售 1是 0否")
    private Integer sellableFlag;
}
