package com.tides.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Schema(title = "ProjectDetailVo", description = "Professional project center detail")
public class ProjectDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private ProjectManageVo overview;

    private ProjectLifecycleVo lifecycle;

    private List<ProjectLifecycleRecordVo> lifecycleRecordList;

    private List<ProjectApprovalOrderVo> approvalOrderList;

    private List<ProjectOperationLogVo> operationLogList;

    private ProjectBaseInfo baseInfo;

    private ContentInfo contentInfo;

    private TicketInfo ticketInfo;

    private ScreeningInfo screeningInfo;

    private ComplianceInfo complianceInfo;

    private List<ChecklistItem> checklist;

    private List<RiskItem> riskItems;

    private List<OperationAction> operationActions;

    @Data
    public static class ProjectBaseInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long projectId;

        private String title;

        private String projectTypeName;

        private Long parentProgramCategoryId;

        private String parentProgramCategoryName;

        private Long programCategoryId;

        private String programCategoryName;

        private Long areaId;

        private String actor;

        private String mainActor;

        private String place;

        private String itemPicture;

        private String detail;

        private Integer programStatus;

        private String programStatusName;

        private Integer lifecycleStatus;

        private String lifecycleStatusName;

        private Integer preSell;

        private String preSellInstruction;

        private String importantNotice;

        private Date issueTime;

        private Date createTime;

        private Date editTime;
    }

    @Data
    public static class ContentInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long movieId;

        private String movieName;

        private String movieAlias;

        private String director;

        private String actors;

        private Integer durationMinutes;

        private String language;

        private String region;

        private Date releaseDate;

        private String poster;

        private String genre;

        private Integer releaseStatus;

        private String releaseStatusName;

        private Long wantWatchCount;

        private Long watchedCount;

        private BigDecimal ratingScore;

        private BigDecimal boxOfficeAmount;

        private String producer;

        private String distributor;

        private String ageTips;

        private Long artistCount;

        private Long mediaCount;

        private Long trailerCount;

        private Long stillCount;

        private Long posterCount;

        private List<ArtistItem> artistList;
    }

    @Data
    public static class ArtistItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long artistId;

        private String artistName;

        private String roleType;

        private String roleName;

        private Integer displayFlag;

        private Integer sortOrder;
    }

    @Data
    public static class TicketInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long ticketCategoryCount;

        private Long totalNumber;

        private Long totalRemainNumber;

        private BigDecimal minPrice;

        private BigDecimal maxPrice;

        private Integer perOrderLimitPurchaseCount;

        private Integer perAccountLimitPurchaseCount;

        private Integer permitRefund;

        private Integer relNameTicketEntrance;

        private Integer permitChooseSeat;

        private Integer electronicDeliveryTicket;

        private Integer electronicInvoice;

        private String refundTicketRule;

        private String refundExplain;

        private String deliveryInstruction;

        private String entryRule;

        private String realTicketPurchaseRule;

        private String relNameTicketEntranceExplain;

        private String chooseSeatExplain;

        private String electronicDeliveryTicketExplain;

        private String electronicInvoiceExplain;

        private List<TicketItem> ticketList;
    }

    @Data
    public static class TicketItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long ticketCategoryId;

        private String introduce;

        private BigDecimal price;

        private Long totalNumber;

        private Long remainNumber;
    }

    @Data
    public static class ScreeningInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long screeningCount;

        private Long activeScreeningCount;

        private Long cinemaCount;

        private Long hallCount;

        private Long openInventoryIssueCount;

        private Date firstShowTime;

        private Date lastShowTime;

        private Date nextShowTime;

        private List<ScreeningPreview> upcomingScreeningList;
    }

    @Data
    public static class ScreeningPreview implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long screeningId;

        private Long cinemaId;

        private String cinemaName;

        private String cityName;

        private String districtName;

        private String address;

        private Long hallId;

        private String hallName;

        private String hallType;

        private Date showTime;

        private Date endTime;

        private String language;

        private String version;

        private BigDecimal lowestPrice;

        private Integer screeningStatus;

        private String screeningStatusName;

        private Long dbRemainNumber;
    }

    @Data
    public static class ComplianceInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String refundTicketRule;

        private String refundExplain;

        private String deliveryInstruction;

        private String entryRule;

        private String realTicketPurchaseRule;

        private String childPurchase;

        private String invoiceSpecification;

        private String abnormalOrderDescription;

        private String kindReminder;

        private String prohibitedItem;

        private String depositSpecification;

        private String performanceDuration;

        private String entryTime;

        private Integer relNameTicketEntrance;

        private String relNameTicketEntranceExplain;

        private Integer permitChooseSeat;

        private String chooseSeatExplain;

        private Integer electronicDeliveryTicket;

        private String electronicDeliveryTicketExplain;

        private Integer electronicInvoice;

        private String electronicInvoiceExplain;
    }

    @Data
    public static class ChecklistItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String groupName;

        private String itemName;

        private Boolean completed;

        private String professionalBenchmark;

        private String advice;
    }

    @Data
    public static class RiskItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String riskType;

        private String riskLevel;

        private String problemDesc;

        private String nextAction;
    }

    @Data
    public static class OperationAction implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String actionType;

        private String actionName;

        private String targetCenter;

        private String description;

        private Integer sortOrder;
    }
}
