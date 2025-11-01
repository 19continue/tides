package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @description: 购票人订单信息 vo
 * @author: 19continue
 **/
@Data
@Schema(title="OrderTicketUserManageVo", description ="购票人订单信息")
public class OrderTicketUserManageVo {

    /**
     * 主键id
     */
    @Schema(name ="id", type ="Long", description ="购票人订单id")
    private Long id;
    
    @Schema(name ="orderId", type ="Long", description ="订单id")
    private Long orderId;
    
    @Schema(name ="programId", type ="Long", description ="节目表id")
    private Long programId;

    @Schema(name ="screeningId", type ="Long", description ="movie screening id")
    private Long screeningId;

    @Schema(name ="userId", type ="Long", description ="用户id")
    private Long userId;
    
    @Schema(name ="ticketUserId", type ="Long", description ="购票人id")
    private Long ticketUserId;
    
    @Schema(name ="seatId", type ="Long", description ="座位id")
    private Long seatId;
    
    @Schema(name ="seatInfo", type ="String", description ="座位信息")
    private String seatInfo;
    
    @Schema(name ="orderPrice", type ="BigDecimal", description ="订单价格")
    private BigDecimal orderPrice;
    
    @Schema(name ="orderStatus", type ="Integer", description ="订单状态 1:未支付 2:已取消 3:已支付 4:已退单")
    private Integer orderStatus;
    
    @Schema(name ="orderStatusName", type ="orderStatusName", description ="订单状态名字")
    private String orderStatusName;

    @Schema(name ="ticketCode", type ="String", description ="电子票码")
    private String ticketCode;

    @Schema(name ="ticketStatus", type ="Integer", description ="电子票状态")
    private Integer ticketStatus;

    @Schema(name ="ticketStatusName", type ="String", description ="电子票状态名字")
    private String ticketStatusName;
    
    @Schema(name ="createOrderTime", type ="Date", description ="生成订单时间")
    private Date createOrderTime;
    
    @Schema(name ="cancelOrderTime", type ="Date", description ="取消订单时间")
    private Date cancelOrderTime;
    
    @Schema(name ="payOrderTime", type ="Date", description ="支付订单时间")
    private Date payOrderTime;

    @Schema(name ="ticketIssueTime", type ="Date", description ="出票时间")
    private Date ticketIssueTime;

    @Schema(name ="ticketVerifyTime", type ="Date", description ="核销时间")
    private Date ticketVerifyTime;

    @Schema(name ="refundApplyTime", type ="Date", description ="退款申请时间")
    private Date refundApplyTime;

    @Schema(name ="refundTime", type ="Date", description ="退款完成时间")
    private Date refundTime;

    @Schema(name ="refundReason", type ="String", description ="退款原因")
    private String refundReason;
}
