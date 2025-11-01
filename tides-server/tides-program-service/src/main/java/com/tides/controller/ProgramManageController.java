package com.tides.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tides.common.ApiResponse;
import com.tides.dto.ArtistManageDto;
import com.tides.dto.ArtistSaveDto;
import com.tides.dto.ArtistStatusUpdateDto;
import com.tides.dto.CinemaManageDto;
import com.tides.dto.CinemaSaveDto;
import com.tides.dto.CinemaStatusUpdateDto;
import com.tides.dto.HallManageDto;
import com.tides.dto.HallSaveDto;
import com.tides.dto.HallSeatBatchGenerateDto;
import com.tides.dto.HallSeatPageManageDto;
import com.tides.dto.HallSeatSaveDto;
import com.tides.dto.HallSeatStatusUpdateDto;
import com.tides.dto.HallStatusUpdateDto;
import com.tides.dto.MovieManageDto;
import com.tides.dto.MovieInventoryEventManageDto;
import com.tides.dto.MovieInventoryReconcileDto;
import com.tides.dto.MovieInventoryReconcileIssueHandleDto;
import com.tides.dto.MovieInventoryReconcileIssueManageDto;
import com.tides.dto.MovieMediaManageDto;
import com.tides.dto.MovieMediaSaveDto;
import com.tides.dto.MovieMediaStatusUpdateDto;
import com.tides.dto.MovieProfileManageDto;
import com.tides.dto.MovieProfileSaveDto;
import com.tides.dto.MovieProfileStatusUpdateDto;
import com.tides.dto.MovieSaveDto;
import com.tides.dto.MovieScreeningManageDto;
import com.tides.dto.MovieScreeningPriceSaveDto;
import com.tides.dto.MovieScreeningPriceManageDto;
import com.tides.dto.MovieScreeningPriceStatusUpdateDto;
import com.tides.dto.MovieScreeningSaveDto;
import com.tides.dto.MovieScreeningSeatGenerateDto;
import com.tides.dto.MovieScreeningSeatPageManageDto;
import com.tides.dto.MovieScreeningSeatStatusUpdateDto;
import com.tides.dto.MovieScreeningStatusUpdateDto;
import com.tides.dto.MovieStatusUpdateDto;
import com.tides.dto.ProgramArtistManageDto;
import com.tides.dto.ProgramArtistSaveDto;
import com.tides.dto.ProgramArtistStatusUpdateDto;
import com.tides.dto.ProgramManageDto;
import com.tides.dto.ProjectApprovalHandleDto;
import com.tides.dto.ProjectApprovalManageDto;
import com.tides.dto.ProjectApprovalPolicyChangeLogManageDto;
import com.tides.dto.ProjectApprovalPolicyManageDto;
import com.tides.dto.ProjectApprovalPolicySaveDto;
import com.tides.dto.ProjectApprovalPolicyStatusUpdateDto;
import com.tides.dto.ProjectBaseInfoSaveDto;
import com.tides.dto.ProjectDetailDto;
import com.tides.dto.ProjectLifecycleTransitionDto;
import com.tides.dto.ProjectManageDto;
import com.tides.dto.ProjectOperationLogManageDto;
import com.tides.dto.ProjectRuleSaveDto;
import com.tides.dto.SeatPageManageDto;
import com.tides.dto.VenueManageDto;
import com.tides.dto.VenueSaveDto;
import com.tides.dto.VenueStatusUpdateDto;
import com.tides.service.MovieScreeningManageService;
import com.tides.service.OperationMasterManageService;
import com.tides.service.ProgramManageService;
import com.tides.service.ProjectAuditLogService;
import com.tides.service.ProjectApprovalPolicyService;
import com.tides.service.ProjectApprovalService;
import com.tides.service.ProjectEditService;
import com.tides.service.ProjectManageService;
import com.tides.vo.ArtistManageVo;
import com.tides.vo.CinemaManageVo;
import com.tides.vo.HallManageVo;
import com.tides.vo.HallSeatManageVo;
import com.tides.vo.MovieInventoryEventManageVo;
import com.tides.vo.MovieInventoryReconcileIssueManageVo;
import com.tides.vo.MovieInventoryReconcileVo;
import com.tides.vo.MovieManageVo;
import com.tides.vo.MovieMediaManageVo;
import com.tides.vo.MovieProfileManageVo;
import com.tides.vo.MovieScreeningManageVo;
import com.tides.vo.MovieScreeningPriceManageVo;
import com.tides.vo.MovieScreeningSeatManageVo;
import com.tides.vo.ProgramArtistManageVo;
import com.tides.vo.ProjectApprovalOrderVo;
import com.tides.vo.ProjectApprovalPolicyChangeLogVo;
import com.tides.vo.ProjectApprovalPolicyVo;
import com.tides.vo.ProjectDetailVo;
import com.tides.vo.ProjectManageVo;
import com.tides.vo.ProjectOperationLogVo;
import com.tides.vo.SeatManageVo;
import com.tides.vo.TicketCategoryDbManageVo;
import com.tides.vo.TicketCategoryDetailManageVo;
import com.tides.vo.VenueManageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description: 节目后台管理 控制层
 * @author: 19continue
 **/
@RestController
@RequestMapping("/program/manage")
@Tag(name = "program/manage", description = "节目后台管理")
public class ProgramManageController {
    
    
    @Autowired
    private ProgramManageService programManageService;

    @Autowired
    private ProjectManageService projectManageService;

    @Autowired
    private ProjectEditService projectEditService;

    @Autowired
    private ProjectAuditLogService projectAuditLogService;

    @Autowired
    private ProjectApprovalService projectApprovalService;

    @Autowired
    private ProjectApprovalPolicyService projectApprovalPolicyService;

    @Autowired
    private OperationMasterManageService operationMasterManageService;

    @Autowired
    private MovieScreeningManageService movieScreeningManageService;
    
    @Operation(summary  = "查询节目票档信息集合")
    @PostMapping(value = "/ticket/category/list")
    public ApiResponse<List<TicketCategoryDetailManageVo>> ticketCategoryList(@Valid @RequestBody ProgramManageDto programManageDto) {
        return ApiResponse.ok(programManageService.ticketCategoryList(programManageDto));
    }
    
    @Operation(summary  = "查询数据库节目票档信息集合")
    @PostMapping(value = "/db/ticket/category/list")
    public ApiResponse<List<TicketCategoryDbManageVo>> dbTicketCategoryList(@Valid @RequestBody ProgramManageDto programManageDto) {
        return ApiResponse.ok(programManageService.dbTicketCategoryList(programManageDto));
    }
    
    @Operation(summary  = "查询节目座位信息集合")
    @PostMapping(value = "/seat/page")
    public ApiResponse<IPage<SeatManageVo>> seatPage(@Valid @RequestBody SeatPageManageDto seatPageManageDto) {
        return ApiResponse.ok(programManageService.seatPage(seatPageManageDto));
    }

    @Operation(summary  = "Professional project center page")
    @PostMapping(value = "/project/page")
    public ApiResponse<IPage<ProjectManageVo>> projectPage(@Valid @RequestBody ProjectManageDto projectManageDto) {
        return ApiResponse.ok(projectManageService.projectPage(projectManageDto));
    }

    @Operation(summary  = "Professional project center detail")
    @PostMapping(value = "/project/detail")
    public ApiResponse<ProjectDetailVo> projectDetail(@Valid @RequestBody ProjectDetailDto projectDetailDto) {
        return ApiResponse.ok(projectManageService.projectDetail(projectDetailDto));
    }

    @Operation(summary  = "Professional project lifecycle transition")
    @PostMapping(value = "/project/lifecycle/transition")
    public ApiResponse<ProjectDetailVo> projectLifecycleTransition(
            @Valid @RequestBody ProjectLifecycleTransitionDto projectLifecycleTransitionDto) {
        return ApiResponse.ok(projectManageService.projectLifecycleTransition(projectLifecycleTransitionDto));
    }

    @Operation(summary  = "Save professional project base information")
    @PostMapping(value = "/project/base/save")
    public ApiResponse<ProjectDetailVo> projectBaseInfoSave(@Valid @RequestBody ProjectBaseInfoSaveDto projectBaseInfoSaveDto) {
        return ApiResponse.ok(projectEditService.projectBaseInfoSave(projectBaseInfoSaveDto));
    }

    @Operation(summary  = "Save professional project ticketing and fulfillment rules")
    @PostMapping(value = "/project/rule/save")
    public ApiResponse<ProjectDetailVo> projectRuleSave(@Valid @RequestBody ProjectRuleSaveDto projectRuleSaveDto) {
        return ApiResponse.ok(projectEditService.projectRuleSave(projectRuleSaveDto));
    }

    @Operation(summary  = "Professional project operation audit log page")
    @PostMapping(value = "/project/operation/log/page")
    public ApiResponse<IPage<ProjectOperationLogVo>> projectOperationLogPage(
            @Valid @RequestBody ProjectOperationLogManageDto projectOperationLogManageDto) {
        return ApiResponse.ok(projectAuditLogService.projectOperationLogPage(projectOperationLogManageDto));
    }

    @Operation(summary  = "Professional project high risk approval page")
    @PostMapping(value = "/project/approval/page")
    public ApiResponse<IPage<ProjectApprovalOrderVo>> projectApprovalPage(
            @Valid @RequestBody ProjectApprovalManageDto projectApprovalManageDto) {
        return ApiResponse.ok(projectApprovalService.projectApprovalPage(projectApprovalManageDto));
    }

    @Operation(summary  = "Handle professional project high risk approval")
    @PostMapping(value = "/project/approval/handle")
    public ApiResponse<ProjectDetailVo> projectApprovalHandle(
            @Valid @RequestBody ProjectApprovalHandleDto projectApprovalHandleDto) {
        return ApiResponse.ok(projectManageService.projectApprovalHandle(projectApprovalHandleDto));
    }

    @Operation(summary  = "Professional project approval policy page")
    @PostMapping(value = "/project/approval/policy/page")
    public ApiResponse<IPage<ProjectApprovalPolicyVo>> projectApprovalPolicyPage(
            @Valid @RequestBody ProjectApprovalPolicyManageDto projectApprovalPolicyManageDto) {
        return ApiResponse.ok(projectApprovalPolicyService.projectApprovalPolicyPage(projectApprovalPolicyManageDto));
    }

    @Operation(summary  = "Save professional project approval policy")
    @PostMapping(value = "/project/approval/policy/save")
    public ApiResponse<Long> projectApprovalPolicySave(
            @Valid @RequestBody ProjectApprovalPolicySaveDto projectApprovalPolicySaveDto) {
        return ApiResponse.ok(projectApprovalPolicyService.projectApprovalPolicySave(projectApprovalPolicySaveDto));
    }

    @Operation(summary  = "Update professional project approval policy status")
    @PostMapping(value = "/project/approval/policy/status/update")
    public ApiResponse<Boolean> projectApprovalPolicyStatusUpdate(
            @Valid @RequestBody ProjectApprovalPolicyStatusUpdateDto projectApprovalPolicyStatusUpdateDto) {
        return ApiResponse.ok(projectApprovalPolicyService.projectApprovalPolicyStatusUpdate(projectApprovalPolicyStatusUpdateDto));
    }

    @Operation(summary  = "Professional project approval policy change log page")
    @PostMapping(value = "/project/approval/policy/change/log/page")
    public ApiResponse<IPage<ProjectApprovalPolicyChangeLogVo>> projectApprovalPolicyChangeLogPage(
            @Valid @RequestBody ProjectApprovalPolicyChangeLogManageDto projectApprovalPolicyChangeLogManageDto) {
        return ApiResponse.ok(projectApprovalPolicyService.projectApprovalPolicyChangeLogPage(projectApprovalPolicyChangeLogManageDto));
    }

    @Operation(summary  = "Movie master data page")
    @PostMapping(value = "/movie/page")
    public ApiResponse<IPage<MovieManageVo>> moviePage(@Valid @RequestBody MovieManageDto movieManageDto) {
        return ApiResponse.ok(operationMasterManageService.moviePage(movieManageDto));
    }

    @Operation(summary  = "Save movie master data")
    @PostMapping(value = "/movie/save")
    public ApiResponse<Long> movieSave(@Valid @RequestBody MovieSaveDto movieSaveDto) {
        return ApiResponse.ok(operationMasterManageService.movieSave(movieSaveDto));
    }

    @Operation(summary  = "Update movie master data status")
    @PostMapping(value = "/movie/status/update")
    public ApiResponse<Boolean> movieStatusUpdate(@Valid @RequestBody MovieStatusUpdateDto movieStatusUpdateDto) {
        return ApiResponse.ok(operationMasterManageService.movieStatusUpdate(movieStatusUpdateDto));
    }

    @Operation(summary  = "Cinema master data page")
    @PostMapping(value = "/cinema/page")
    public ApiResponse<IPage<CinemaManageVo>> cinemaPage(@Valid @RequestBody CinemaManageDto cinemaManageDto) {
        return ApiResponse.ok(operationMasterManageService.cinemaPage(cinemaManageDto));
    }

    @Operation(summary  = "Save cinema master data")
    @PostMapping(value = "/cinema/save")
    public ApiResponse<Long> cinemaSave(@Valid @RequestBody CinemaSaveDto cinemaSaveDto) {
        return ApiResponse.ok(operationMasterManageService.cinemaSave(cinemaSaveDto));
    }

    @Operation(summary  = "Update cinema master data status")
    @PostMapping(value = "/cinema/status/update")
    public ApiResponse<Boolean> cinemaStatusUpdate(@Valid @RequestBody CinemaStatusUpdateDto cinemaStatusUpdateDto) {
        return ApiResponse.ok(operationMasterManageService.cinemaStatusUpdate(cinemaStatusUpdateDto));
    }

    @Operation(summary  = "Hall master data page")
    @PostMapping(value = "/hall/page")
    public ApiResponse<IPage<HallManageVo>> hallPage(@Valid @RequestBody HallManageDto hallManageDto) {
        return ApiResponse.ok(operationMasterManageService.hallPage(hallManageDto));
    }

    @Operation(summary  = "Save hall master data")
    @PostMapping(value = "/hall/save")
    public ApiResponse<Long> hallSave(@Valid @RequestBody HallSaveDto hallSaveDto) {
        return ApiResponse.ok(operationMasterManageService.hallSave(hallSaveDto));
    }

    @Operation(summary  = "Update hall master data status")
    @PostMapping(value = "/hall/status/update")
    public ApiResponse<Boolean> hallStatusUpdate(@Valid @RequestBody HallStatusUpdateDto hallStatusUpdateDto) {
        return ApiResponse.ok(operationMasterManageService.hallStatusUpdate(hallStatusUpdateDto));
    }

    @Operation(summary  = "Hall seat template page")
    @PostMapping(value = "/hall/seat/page")
    public ApiResponse<IPage<HallSeatManageVo>> hallSeatPage(@Valid @RequestBody HallSeatPageManageDto hallSeatPageManageDto) {
        return ApiResponse.ok(operationMasterManageService.hallSeatPage(hallSeatPageManageDto));
    }

    @Operation(summary  = "Save hall seat template")
    @PostMapping(value = "/hall/seat/save")
    public ApiResponse<Long> hallSeatSave(@Valid @RequestBody HallSeatSaveDto hallSeatSaveDto) {
        return ApiResponse.ok(operationMasterManageService.hallSeatSave(hallSeatSaveDto));
    }

    @Operation(summary  = "Batch generate hall seat template")
    @PostMapping(value = "/hall/seat/batch/generate")
    public ApiResponse<Integer> hallSeatBatchGenerate(@Valid @RequestBody HallSeatBatchGenerateDto hallSeatBatchGenerateDto) {
        return ApiResponse.ok(operationMasterManageService.hallSeatBatchGenerate(hallSeatBatchGenerateDto));
    }

    @Operation(summary  = "Update hall seat template status")
    @PostMapping(value = "/hall/seat/status/update")
    public ApiResponse<Boolean> hallSeatStatusUpdate(@Valid @RequestBody HallSeatStatusUpdateDto hallSeatStatusUpdateDto) {
        return ApiResponse.ok(operationMasterManageService.hallSeatStatusUpdate(hallSeatStatusUpdateDto));
    }

    @Operation(summary  = "Venue master data page")
    @PostMapping(value = "/venue/page")
    public ApiResponse<IPage<VenueManageVo>> venuePage(@Valid @RequestBody VenueManageDto venueManageDto) {
        return ApiResponse.ok(operationMasterManageService.venuePage(venueManageDto));
    }

    @Operation(summary  = "Save venue master data")
    @PostMapping(value = "/venue/save")
    public ApiResponse<Long> venueSave(@Valid @RequestBody VenueSaveDto venueSaveDto) {
        return ApiResponse.ok(operationMasterManageService.venueSave(venueSaveDto));
    }

    @Operation(summary  = "Update venue master data status")
    @PostMapping(value = "/venue/status/update")
    public ApiResponse<Boolean> venueStatusUpdate(@Valid @RequestBody VenueStatusUpdateDto venueStatusUpdateDto) {
        return ApiResponse.ok(operationMasterManageService.venueStatusUpdate(venueStatusUpdateDto));
    }

    @Operation(summary  = "Artist master data page")
    @PostMapping(value = "/artist/page")
    public ApiResponse<IPage<ArtistManageVo>> artistPage(@Valid @RequestBody ArtistManageDto artistManageDto) {
        return ApiResponse.ok(operationMasterManageService.artistPage(artistManageDto));
    }

    @Operation(summary  = "Save artist master data")
    @PostMapping(value = "/artist/save")
    public ApiResponse<Long> artistSave(@Valid @RequestBody ArtistSaveDto artistSaveDto) {
        return ApiResponse.ok(operationMasterManageService.artistSave(artistSaveDto));
    }

    @Operation(summary  = "Update artist master data status")
    @PostMapping(value = "/artist/status/update")
    public ApiResponse<Boolean> artistStatusUpdate(@Valid @RequestBody ArtistStatusUpdateDto artistStatusUpdateDto) {
        return ApiResponse.ok(operationMasterManageService.artistStatusUpdate(artistStatusUpdateDto));
    }

    @Operation(summary  = "Program artist relation page")
    @PostMapping(value = "/program/artist/page")
    public ApiResponse<IPage<ProgramArtistManageVo>> programArtistPage(@Valid @RequestBody ProgramArtistManageDto programArtistManageDto) {
        return ApiResponse.ok(operationMasterManageService.programArtistPage(programArtistManageDto));
    }

    @Operation(summary  = "Save program artist relation")
    @PostMapping(value = "/program/artist/save")
    public ApiResponse<Long> programArtistSave(@Valid @RequestBody ProgramArtistSaveDto programArtistSaveDto) {
        return ApiResponse.ok(operationMasterManageService.programArtistSave(programArtistSaveDto));
    }

    @Operation(summary  = "Update program artist relation status")
    @PostMapping(value = "/program/artist/status/update")
    public ApiResponse<Boolean> programArtistStatusUpdate(@Valid @RequestBody ProgramArtistStatusUpdateDto programArtistStatusUpdateDto) {
        return ApiResponse.ok(operationMasterManageService.programArtistStatusUpdate(programArtistStatusUpdateDto));
    }

    @Operation(summary  = "Movie profile page")
    @PostMapping(value = "/movie/profile/page")
    public ApiResponse<IPage<MovieProfileManageVo>> movieProfilePage(@Valid @RequestBody MovieProfileManageDto movieProfileManageDto) {
        return ApiResponse.ok(operationMasterManageService.movieProfilePage(movieProfileManageDto));
    }

    @Operation(summary  = "Save movie profile")
    @PostMapping(value = "/movie/profile/save")
    public ApiResponse<Long> movieProfileSave(@Valid @RequestBody MovieProfileSaveDto movieProfileSaveDto) {
        return ApiResponse.ok(operationMasterManageService.movieProfileSave(movieProfileSaveDto));
    }

    @Operation(summary  = "Update movie profile status")
    @PostMapping(value = "/movie/profile/status/update")
    public ApiResponse<Boolean> movieProfileStatusUpdate(@Valid @RequestBody MovieProfileStatusUpdateDto movieProfileStatusUpdateDto) {
        return ApiResponse.ok(operationMasterManageService.movieProfileStatusUpdate(movieProfileStatusUpdateDto));
    }

    @Operation(summary  = "Movie media page")
    @PostMapping(value = "/movie/media/page")
    public ApiResponse<IPage<MovieMediaManageVo>> movieMediaPage(@Valid @RequestBody MovieMediaManageDto movieMediaManageDto) {
        return ApiResponse.ok(operationMasterManageService.movieMediaPage(movieMediaManageDto));
    }

    @Operation(summary  = "Save movie media")
    @PostMapping(value = "/movie/media/save")
    public ApiResponse<Long> movieMediaSave(@Valid @RequestBody MovieMediaSaveDto movieMediaSaveDto) {
        return ApiResponse.ok(operationMasterManageService.movieMediaSave(movieMediaSaveDto));
    }

    @Operation(summary  = "Update movie media status")
    @PostMapping(value = "/movie/media/status/update")
    public ApiResponse<Boolean> movieMediaStatusUpdate(@Valid @RequestBody MovieMediaStatusUpdateDto movieMediaStatusUpdateDto) {
        return ApiResponse.ok(operationMasterManageService.movieMediaStatusUpdate(movieMediaStatusUpdateDto));
    }

    @Operation(summary  = "查询电影场次分页列表")
    @PostMapping(value = "/movie/screening/page")
    public ApiResponse<IPage<MovieScreeningManageVo>> movieScreeningPage(@Valid @RequestBody MovieScreeningManageDto movieScreeningManageDto) {
        return ApiResponse.ok(movieScreeningManageService.movieScreeningPage(movieScreeningManageDto));
    }

    @Operation(summary  = "查询电影场次票价信息集合")
    @PostMapping(value = "/movie/screening/price/list")
    public ApiResponse<List<MovieScreeningPriceManageVo>> movieScreeningPriceList(@Valid @RequestBody MovieScreeningPriceManageDto movieScreeningPriceManageDto) {
        return ApiResponse.ok(movieScreeningManageService.movieScreeningPriceList(movieScreeningPriceManageDto));
    }

    @Operation(summary  = "查询电影场次座位信息集合")
    @PostMapping(value = "/movie/screening/seat/page")
    public ApiResponse<IPage<MovieScreeningSeatManageVo>> movieScreeningSeatPage(@Valid @RequestBody MovieScreeningSeatPageManageDto movieScreeningSeatPageManageDto) {
        return ApiResponse.ok(movieScreeningManageService.movieScreeningSeatPage(movieScreeningSeatPageManageDto));
    }

    @Operation(summary  = "查询电影库存变更流水")
    @PostMapping(value = "/movie/inventory/event/page")
    public ApiResponse<IPage<MovieInventoryEventManageVo>> movieInventoryEventPage(@Valid @RequestBody MovieInventoryEventManageDto movieInventoryEventManageDto) {
        return ApiResponse.ok(movieScreeningManageService.movieInventoryEventPage(movieInventoryEventManageDto));
    }

    @Operation(summary  = "电影场次库存对账诊断")
    @PostMapping(value = "/movie/inventory/reconcile")
    public ApiResponse<MovieInventoryReconcileVo> movieInventoryReconcile(@Valid @RequestBody MovieInventoryReconcileDto movieInventoryReconcileDto) {
        return ApiResponse.ok(movieScreeningManageService.movieInventoryReconcile(movieInventoryReconcileDto));
    }

    @Operation(summary  = "Movie inventory reconcile issue page")
    @PostMapping(value = "/movie/inventory/reconcile/issue/page")
    public ApiResponse<IPage<MovieInventoryReconcileIssueManageVo>> movieInventoryReconcileIssuePage(
            @Valid @RequestBody MovieInventoryReconcileIssueManageDto movieInventoryReconcileIssueManageDto) {
        return ApiResponse.ok(movieScreeningManageService.movieInventoryReconcileIssuePage(movieInventoryReconcileIssueManageDto));
    }

    @Operation(summary  = "Handle movie inventory reconcile issue")
    @PostMapping(value = "/movie/inventory/reconcile/issue/handle")
    public ApiResponse<Boolean> movieInventoryReconcileIssueHandle(
            @Valid @RequestBody MovieInventoryReconcileIssueHandleDto movieInventoryReconcileIssueHandleDto) {
        return ApiResponse.ok(movieScreeningManageService.movieInventoryReconcileIssueHandle(movieInventoryReconcileIssueHandleDto));
    }

    @Operation(summary  = "Save movie screening")
    @PostMapping(value = "/movie/screening/save")
    public ApiResponse<Long> movieScreeningSave(@Valid @RequestBody MovieScreeningSaveDto movieScreeningSaveDto) {
        return ApiResponse.ok(movieScreeningManageService.movieScreeningSave(movieScreeningSaveDto));
    }

    @Operation(summary  = "更新电影放映场次状态")
    @PostMapping(value = "/movie/screening/status/update")
    public ApiResponse<Boolean> movieScreeningStatusUpdate(@Valid @RequestBody MovieScreeningStatusUpdateDto movieScreeningStatusUpdateDto) {
        return ApiResponse.ok(movieScreeningManageService.movieScreeningStatusUpdate(movieScreeningStatusUpdateDto));
    }

    @Operation(summary  = "保存电影场次票价库存")
    @PostMapping(value = "/movie/screening/price/save")
    public ApiResponse<Long> movieScreeningPriceSave(@Valid @RequestBody MovieScreeningPriceSaveDto movieScreeningPriceSaveDto) {
        return ApiResponse.ok(movieScreeningManageService.movieScreeningPriceSave(movieScreeningPriceSaveDto));
    }

    @Operation(summary  = "更新电影场次票价状态")
    @PostMapping(value = "/movie/screening/price/status/update")
    public ApiResponse<Boolean> movieScreeningPriceStatusUpdate(@Valid @RequestBody MovieScreeningPriceStatusUpdateDto movieScreeningPriceStatusUpdateDto) {
        return ApiResponse.ok(movieScreeningManageService.movieScreeningPriceStatusUpdate(movieScreeningPriceStatusUpdateDto));
    }

    @Operation(summary  = "按影厅模板生成电影场次座位")
    @PostMapping(value = "/movie/screening/seat/generate")
    public ApiResponse<Integer> movieScreeningSeatGenerate(@Valid @RequestBody MovieScreeningSeatGenerateDto movieScreeningSeatGenerateDto) {
        return ApiResponse.ok(movieScreeningManageService.movieScreeningSeatGenerate(movieScreeningSeatGenerateDto));
    }

    @Operation(summary  = "更新电影场次座位售卖状态")
    @PostMapping(value = "/movie/screening/seat/status/update")
    public ApiResponse<Boolean> movieScreeningSeatStatusUpdate(@Valid @RequestBody MovieScreeningSeatStatusUpdateDto movieScreeningSeatStatusUpdateDto) {
        return ApiResponse.ok(movieScreeningManageService.movieScreeningSeatStatusUpdate(movieScreeningSeatStatusUpdateDto));
    }
}
