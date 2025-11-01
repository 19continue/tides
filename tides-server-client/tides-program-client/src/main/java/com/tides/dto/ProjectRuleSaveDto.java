package com.tides.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(title = "ProjectRuleSaveDto", description = "Project ticketing and fulfillment rule save")
public class ProjectRuleSaveDto {

    @Schema(name = "projectId", type = "Long", description = "project/program id", requiredMode = RequiredMode.REQUIRED)
    @NotNull
    private Long projectId;

    @Schema(name = "operatorId", type = "Long", description = "operator id")
    private Long operatorId;

    @Schema(name = "operatorName", type = "String", description = "operator name")
    @Size(max = 128)
    private String operatorName;

    @Schema(name = "remark", type = "String", description = "operation remark")
    @Size(max = 512)
    private String remark;

    @Schema(name = "perOrderLimitPurchaseCount", type = "Integer", description = "per order purchase limit")
    private Integer perOrderLimitPurchaseCount;

    @Schema(name = "perAccountLimitPurchaseCount", type = "Integer", description = "per account purchase limit")
    private Integer perAccountLimitPurchaseCount;

    @Schema(name = "permitRefund", type = "Integer", description = "0 no refund, 1 conditional, 2 all")
    private Integer permitRefund;

    @Schema(name = "refundTicketRule", type = "String", description = "refund or exchange rule")
    @Size(max = 512)
    private String refundTicketRule;

    @Schema(name = "refundExplain", type = "String", description = "refund explanation")
    @Size(max = 512)
    private String refundExplain;

    @Schema(name = "deliveryInstruction", type = "String", description = "delivery instruction")
    @Size(max = 512)
    private String deliveryInstruction;

    @Schema(name = "entryRule", type = "String", description = "entry rule")
    @Size(max = 512)
    private String entryRule;

    @Schema(name = "realTicketPurchaseRule", type = "String", description = "real-name purchase rule")
    private String realTicketPurchaseRule;

    @Schema(name = "childPurchase", type = "String", description = "child purchase rule")
    @Size(max = 512)
    private String childPurchase;

    @Schema(name = "invoiceSpecification", type = "String", description = "invoice specification")
    @Size(max = 512)
    private String invoiceSpecification;

    @Schema(name = "abnormalOrderDescription", type = "String", description = "abnormal order description")
    private String abnormalOrderDescription;

    @Schema(name = "kindReminder", type = "String", description = "kind reminder")
    private String kindReminder;

    @Schema(name = "prohibitedItem", type = "String", description = "prohibited item")
    private String prohibitedItem;

    @Schema(name = "depositSpecification", type = "String", description = "deposit specification")
    @Size(max = 512)
    private String depositSpecification;

    @Schema(name = "performanceDuration", type = "String", description = "performance duration")
    @Size(max = 100)
    private String performanceDuration;

    @Schema(name = "entryTime", type = "String", description = "entry time")
    @Size(max = 512)
    private String entryTime;

    @Schema(name = "relNameTicketEntrance", type = "Integer", description = "real-name entry flag")
    private Integer relNameTicketEntrance;

    @Schema(name = "relNameTicketEntranceExplain", type = "String", description = "real-name entry explanation")
    @Size(max = 512)
    private String relNameTicketEntranceExplain;

    @Schema(name = "permitChooseSeat", type = "Integer", description = "choose seat flag")
    private Integer permitChooseSeat;

    @Schema(name = "chooseSeatExplain", type = "String", description = "choose seat explanation")
    @Size(max = 512)
    private String chooseSeatExplain;

    @Schema(name = "electronicDeliveryTicket", type = "Integer", description = "0 none, 1 electronic, 2 delivery")
    private Integer electronicDeliveryTicket;

    @Schema(name = "electronicDeliveryTicketExplain", type = "String", description = "electronic or delivery explanation")
    @Size(max = 512)
    private String electronicDeliveryTicketExplain;

    @Schema(name = "electronicInvoice", type = "Integer", description = "electronic invoice flag")
    private Integer electronicInvoice;

    @Schema(name = "electronicInvoiceExplain", type = "String", description = "electronic invoice explanation")
    @Size(max = 512)
    private String electronicInvoiceExplain;
}
