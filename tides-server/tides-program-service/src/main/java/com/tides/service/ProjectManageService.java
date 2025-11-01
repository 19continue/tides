package com.tides.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tides.dto.ProjectApprovalHandleDto;
import com.tides.dto.ProjectDetailDto;
import com.tides.dto.ProjectLifecycleTransitionDto;
import com.tides.dto.ProjectManageDto;
import com.tides.entity.Artist;
import com.tides.entity.Cinema;
import com.tides.entity.Hall;
import com.tides.entity.Movie;
import com.tides.entity.MovieInventoryReconcileIssue;
import com.tides.entity.MovieMedia;
import com.tides.entity.MovieProfile;
import com.tides.entity.MovieScreening;
import com.tides.entity.MovieScreeningPrice;
import com.tides.entity.Program;
import com.tides.entity.ProgramArtist;
import com.tides.entity.ProgramCategory;
import com.tides.entity.ProjectApprovalOrder;
import com.tides.entity.ProjectLifecycle;
import com.tides.entity.ProjectLifecycleRecord;
import com.tides.entity.TicketCategory;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.enums.ProjectLifecycleStatus;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.ArtistMapper;
import com.tides.mapper.CinemaMapper;
import com.tides.mapper.HallMapper;
import com.tides.mapper.MovieInventoryReconcileIssueMapper;
import com.tides.mapper.MovieMapper;
import com.tides.mapper.MovieMediaMapper;
import com.tides.mapper.MovieProfileMapper;
import com.tides.mapper.MovieScreeningMapper;
import com.tides.mapper.MovieScreeningPriceMapper;
import com.tides.mapper.ProgramArtistMapper;
import com.tides.mapper.ProgramCategoryMapper;
import com.tides.mapper.ProgramMapper;
import com.tides.mapper.ProjectLifecycleMapper;
import com.tides.mapper.ProjectLifecycleRecordMapper;
import com.tides.mapper.TicketCategoryMapper;
import com.tides.page.PageUtil;
import com.tides.util.DateUtils;
import com.tides.util.StringUtil;
import com.tides.vo.ProjectDetailVo;
import com.tides.vo.ProjectLifecycleRecordVo;
import com.tides.vo.ProjectLifecycleVo;
import com.tides.vo.ProjectManageVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectManageService {

    private static final int RECONCILE_ISSUE_OPEN = 1;
    private static final int PROJECT_REVIEW_NONE = 0;
    private static final int PROJECT_REVIEW_PENDING = 1;
    private static final int PROJECT_REVIEW_APPROVED = 2;
    private static final int PROJECT_REVIEW_REJECTED = 3;

    private static final String ACTION_SUBMIT_REVIEW = "SUBMIT_REVIEW";
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT = "REJECT";
    private static final String ACTION_READY_WARMUP = "READY_WARMUP";
    private static final String ACTION_START_PRESELL = "START_PRESELL";
    private static final String ACTION_START_SELL = "START_SELL";
    private static final String ACTION_STOP_SELL = "STOP_SELL";
    private static final String ACTION_MARK_PERFORMING = "MARK_PERFORMING";
    private static final String ACTION_FINISH = "FINISH";
    private static final String ACTION_SETTLE = "SETTLE";
    private static final String ACTION_ARCHIVE = "ARCHIVE";
    private static final String ACTION_POSTPONE = "POSTPONE";
    private static final String ACTION_CANCEL = "CANCEL";
    private static final int LIFECYCLE_REMARK_MIN_LENGTH = 5;
    private static final int LIFECYCLE_REMARK_MAX_LENGTH = 512;

    @Autowired
    private UidGenerator uidGenerator;
    @Autowired
    private ProgramMapper programMapper;
    @Autowired
    private ProgramCategoryMapper programCategoryMapper;
    @Autowired
    private ProjectLifecycleMapper projectLifecycleMapper;
    @Autowired
    private ProjectLifecycleRecordMapper projectLifecycleRecordMapper;
    @Autowired
    private ProjectAuditLogService projectAuditLogService;
    @Autowired
    private ProjectApprovalService projectApprovalService;
    @Autowired
    private BackManageOperatorContextService backManageOperatorContextService;
    @Autowired
    private MovieMapper movieMapper;
    @Autowired
    private MovieProfileMapper movieProfileMapper;
    @Autowired
    private MovieMediaMapper movieMediaMapper;
    @Autowired
    private MovieScreeningMapper movieScreeningMapper;
    @Autowired
    private MovieScreeningPriceMapper movieScreeningPriceMapper;
    @Autowired
    private MovieInventoryReconcileIssueMapper movieInventoryReconcileIssueMapper;
    @Autowired
    private TicketCategoryMapper ticketCategoryMapper;
    @Autowired
    private ProgramArtistMapper programArtistMapper;
    @Autowired
    private ArtistMapper artistMapper;
    @Autowired
    private CinemaMapper cinemaMapper;
    @Autowired
    private HallMapper hallMapper;

    public IPage<ProjectManageVo> projectPage(ProjectManageDto projectManageDto) {
        IPage<ProjectManageVo> result = new Page<>(projectManageDto.getPageNumber(), projectManageDto.getPageSize());
        IPage<Program> programPage = programMapper.selectPage(PageUtil.getPageParams(projectManageDto.getPageNumber(),
                projectManageDto.getPageSize()), Wrappers.lambdaQuery(Program.class)
                .eq(Objects.nonNull(projectManageDto.getProjectId()), Program::getId, projectManageDto.getProjectId())
                .eq(Objects.nonNull(projectManageDto.getParentProgramCategoryId()), Program::getParentProgramCategoryId,
                        projectManageDto.getParentProgramCategoryId())
                .eq(Objects.nonNull(projectManageDto.getProgramCategoryId()), Program::getProgramCategoryId,
                        projectManageDto.getProgramCategoryId())
                .like(StringUtil.isNotEmpty(projectManageDto.getTitle()), Program::getTitle, projectManageDto.getTitle())
                .eq(Program::getStatus, BusinessStatus.YES.getCode())
                .orderByDesc(Program::getEditTime)
                .orderByDesc(Program::getId));
        if (CollectionUtil.isEmpty(programPage.getRecords())) {
            return result;
        }
        Map<Long, ProgramCategory> categoryMap = selectProgramCategoryMap(collectCategoryIds(programPage.getRecords()));
        List<ProjectManageVo> records = programPage.getRecords().stream()
                .map(program -> buildProjectManageVo(program, categoryMap))
                .filter(vo -> Objects.isNull(projectManageDto.getLifecycleStatus()) ||
                        Objects.equals(vo.getLifecycleStatus(), projectManageDto.getLifecycleStatus()))
                .toList();
        BeanUtils.copyProperties(programPage, result);
        result.setRecords(records);
        if (Objects.nonNull(projectManageDto.getLifecycleStatus())) {
            result.setTotal(records.size());
        }
        return result;
    }

    public ProjectDetailVo projectDetail(ProjectDetailDto projectDetailDto) {
        Program program = selectProjectProgram(projectDetailDto.getProjectId());
        Map<Long, ProgramCategory> categoryMap = selectProgramCategoryMap(collectCategoryIds(List.of(program)));
        ProjectManageVo overview = buildProjectManageVo(program, categoryMap);
        ProjectLifecycle lifecycle = ensureProjectLifecycle(program, overview);
        fillOverviewLifecycle(overview, lifecycle);

        Movie movie = selectMovieByProgramId(program.getId());
        MovieProfile profile = Objects.nonNull(movie) ? selectMovieProfile(movie.getId(), program.getId()) : null;
        List<TicketCategory> ticketList = selectActiveTicketCategoryList(program.getId());
        List<MovieMedia> mediaList = Objects.nonNull(movie) ? selectActiveMovieMediaList(movie.getId()) : List.of();
        List<ProgramArtist> artistRelList = selectActiveProgramArtistList(program.getId());

        ProjectDetailVo vo = new ProjectDetailVo();
        vo.setOverview(overview);
        vo.setLifecycle(buildProjectLifecycleVo(lifecycle));
        vo.setLifecycleRecordList(selectProjectLifecycleRecordList(program.getId()));
        vo.setApprovalOrderList(projectApprovalService.selectRecentApprovalOrderList(program.getId()));
        vo.setOperationLogList(projectAuditLogService.selectRecentProjectOperationLogList(program.getId()));
        vo.setBaseInfo(buildProjectBaseInfo(program, overview));
        vo.setContentInfo(buildProjectContentInfo(movie, profile, mediaList, artistRelList));
        vo.setTicketInfo(buildProjectTicketInfo(program, ticketList));
        vo.setScreeningInfo(buildProjectScreeningInfo(program.getId(), overview));
        vo.setComplianceInfo(buildProjectComplianceInfo(program));
        vo.setChecklist(buildProjectChecklist(program, overview, movie, profile, ticketList, mediaList, artistRelList));
        vo.setRiskItems(buildProjectRiskItems(program, overview, movie, profile, ticketList, mediaList, artistRelList));
        vo.setOperationActions(buildProjectOperationActions(overview));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailVo projectLifecycleTransition(ProjectLifecycleTransitionDto dto) {
        Program program = selectProjectProgram(dto.getProgramId());
        ProjectManageVo overview = buildProjectManageVo(program,
                selectProgramCategoryMap(collectCategoryIds(List.of(program))));
        ProjectLifecycle lifecycle = ensureProjectLifecycle(program, overview);
        LifecycleAction action = resolveLifecycleAction(dto.getActionType());
        if (!canTransition(lifecycle.getLifecycleStatus(), action)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        dto.setRemark(normalizeText(dto.getRemark()));
        dto.setConfirmText(normalizeText(dto.getConfirmText()));
        validateLifecycleTransitionRisk(action, dto);
        BackManageOperatorContextService.OperatorContext operatorContext =
                backManageOperatorContextService.resolve(dto.getOperatorId(), dto.getOperatorName());
        if (action.dangerFlag()) {
            projectApprovalService.createLifecycleApproval(program, lifecycle, dto, action.actionName(),
                    action.targetStatus(), operatorContext);
            return projectDetailByProgramId(program.getId());
        }
        return executeLifecycleTransition(program, lifecycle, action, dto, operatorContext);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailVo projectApprovalHandle(ProjectApprovalHandleDto dto) {
        BackManageOperatorContextService.OperatorContext operatorContext =
                backManageOperatorContextService.resolve(dto.getOperatorId(), dto.getOperatorName());
        ProjectApprovalOrder approvalOrder = projectApprovalService.handleApproval(dto, operatorContext);
        if (Objects.equals(approvalOrder.getApprovalStatus(), ProjectApprovalService.APPROVAL_APPROVED)) {
            Program program = selectProjectProgram(approvalOrder.getProgramId());
            ProjectManageVo overview = buildProjectManageVo(program,
                    selectProgramCategoryMap(collectCategoryIds(List.of(program))));
            ProjectLifecycle lifecycle = ensureProjectLifecycle(program, overview);
            LifecycleAction action = resolveLifecycleAction(approvalOrder.getActionType());
            if (!canTransition(lifecycle.getLifecycleStatus(), action)) {
                throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
            }
            ProjectLifecycleTransitionDto transitionDto = buildApprovedLifecycleTransitionDto(approvalOrder);
            return executeLifecycleTransition(program, lifecycle, action, transitionDto, operatorContext);
        }
        return projectDetailByProgramId(approvalOrder.getProgramId());
    }

    private ProjectDetailVo executeLifecycleTransition(Program program, ProjectLifecycle lifecycle, LifecycleAction action,
                                                       ProjectLifecycleTransitionDto dto,
                                                       BackManageOperatorContextService.OperatorContext operatorContext) {
        Integer fromStatus = lifecycle.getLifecycleStatus();
        Date now = DateUtils.now();
        lifecycle.setPreviousLifecycleStatus(fromStatus);
        lifecycle.setLifecycleStatus(action.targetStatus());
        lifecycle.setReviewStatus(resolveReviewStatus(action));
        lifecycle.setReviewRemark(dto.getRemark());
        if (ACTION_APPROVE.equals(action.actionType()) || ACTION_REJECT.equals(action.actionType())) {
            lifecycle.setReviewerId(operatorContext.operatorId());
        }
        lifecycle.setLastActionType(action.actionType());
        lifecycle.setLastActionName(action.actionName());
        lifecycle.setLastOperatorId(operatorContext.operatorId());
        lifecycle.setLastOperatorName(operatorContext.operatorName());
        lifecycle.setLastTransitionTime(now);
        lifecycle.setVersion(Optional.ofNullable(lifecycle.getVersion()).orElse(0) + 1);
        lifecycle.setEditTime(now);
        projectLifecycleMapper.update(lifecycle, Wrappers.lambdaUpdate(ProjectLifecycle.class)
                .eq(ProjectLifecycle::getId, lifecycle.getId())
                .eq(ProjectLifecycle::getProgramId, lifecycle.getProgramId())
                .eq(ProjectLifecycle::getStatus, BusinessStatus.YES.getCode()));
        updateProgramStatusByLifecycle(program, action.targetStatus());
        insertProjectLifecycleRecord(lifecycle, fromStatus, action, dto, operatorContext);
        projectAuditLogService.recordLifecycleTransition(program.getId(), operatorContext.operatorId(),
                operatorContext.operatorName(),
                action.actionType(), action.actionName(), dto.getRemark(), lifecycle, action.dangerFlag());
        return projectDetailByProgramId(program.getId());
    }

    private ProjectDetailVo projectDetailByProgramId(Long programId) {
        ProjectDetailDto detailDto = new ProjectDetailDto();
        detailDto.setProjectId(programId);
        return projectDetail(detailDto);
    }

    private ProjectLifecycleTransitionDto buildApprovedLifecycleTransitionDto(ProjectApprovalOrder approvalOrder) {
        ProjectLifecycleTransitionDto dto = new ProjectLifecycleTransitionDto();
        dto.setProgramId(approvalOrder.getProgramId());
        dto.setActionType(approvalOrder.getActionType());
        dto.setRemark(buildApprovedLifecycleRemark(approvalOrder));
        dto.setConfirmText(approvalOrder.getConfirmText());
        dto.setRiskAccepted(true);
        return dto;
    }

    private String buildApprovedLifecycleRemark(ProjectApprovalOrder approvalOrder) {
        String requestReason = Optional.ofNullable(approvalOrder.getRequestReason()).orElse("");
        String approveRemark = Optional.ofNullable(approvalOrder.getApproveRemark()).orElse("");
        return StringUtil.isNotEmpty(approveRemark) ? requestReason + "；审批意见：" + approveRemark : requestReason;
    }

    private ProjectManageVo buildProjectManageVo(Program program, Map<Long, ProgramCategory> categoryMap) {
        ProjectManageVo vo = new ProjectManageVo();
        vo.setProjectId(program.getId());
        vo.setTitle(program.getTitle());
        vo.setParentProgramCategoryId(program.getParentProgramCategoryId());
        vo.setProgramCategoryId(program.getProgramCategoryId());
        vo.setParentProgramCategoryName(selectCategoryName(categoryMap, program.getParentProgramCategoryId()));
        vo.setProgramCategoryName(selectCategoryName(categoryMap, program.getProgramCategoryId()));
        vo.setProjectTypeName(resolveProjectTypeName(vo.getParentProgramCategoryName(), vo.getProgramCategoryName()));
        vo.setProgramStatus(program.getProgramStatus());
        vo.setProgramStatusName(Objects.equals(program.getProgramStatus(), BusinessStatus.YES.getCode()) ? "上架" : "下架");
        vo.setIssueTime(program.getIssueTime());
        vo.setCreateTime(program.getCreateTime());
        vo.setEditTime(program.getEditTime());

        Movie movie = selectMovieByProgramId(program.getId());
        MovieProfile profile = Objects.nonNull(movie) ? selectMovieProfile(movie.getId(), program.getId()) : null;
        Integer lifecycleStatus = Optional.ofNullable(selectPersistedLifecycleStatus(program.getId()))
                .orElse(resolveProjectLifecycleStatus(program, profile));
        vo.setLifecycleStatus(lifecycleStatus);
        vo.setLifecycleStatusName(ProjectLifecycleStatus.getMsg(lifecycleStatus));

        List<TicketCategory> ticketList = selectActiveTicketCategoryList(program.getId());
        vo.setTicketCategoryCount((long) ticketList.size());
        vo.setTotalRemainNumber(sumLong(ticketList.stream().map(TicketCategory::getRemainNumber).toList()));

        Long screeningCount = 0L;
        Long activeScreeningCount = 0L;
        Long openIssueCount = 0L;
        if (Objects.nonNull(movie)) {
            screeningCount = movieScreeningMapper.selectCount(Wrappers.lambdaQuery(MovieScreening.class)
                    .eq(MovieScreening::getProgramId, program.getId())
                    .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode()));
            activeScreeningCount = movieScreeningMapper.selectCount(Wrappers.lambdaQuery(MovieScreening.class)
                    .eq(MovieScreening::getProgramId, program.getId())
                    .eq(MovieScreening::getScreeningStatus, BusinessStatus.YES.getCode())
                    .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode())
                    .ge(MovieScreening::getShowTime, DateUtils.now()));
            openIssueCount = movieInventoryReconcileIssueMapper.selectCount(Wrappers.lambdaQuery(MovieInventoryReconcileIssue.class)
                    .eq(MovieInventoryReconcileIssue::getProgramId, program.getId())
                    .eq(MovieInventoryReconcileIssue::getIssueStatus, RECONCILE_ISSUE_OPEN)
                    .eq(MovieInventoryReconcileIssue::getStatus, BusinessStatus.YES.getCode()));
        }
        vo.setScreeningCount(screeningCount);
        vo.setActiveScreeningCount(activeScreeningCount);
        vo.setOpenInventoryIssueCount(openIssueCount);
        int completenessScore = calculateProjectCompleteness(program, movie, profile, vo.getTicketCategoryCount(), screeningCount);
        vo.setCompletenessScore(completenessScore);
        vo.setRiskCount(calculateProjectRiskCount(completenessScore, vo));
        vo.setNextAction(resolveProjectNextAction(vo));
        return vo;
    }

    private ProjectDetailVo.ProjectBaseInfo buildProjectBaseInfo(Program program, ProjectManageVo overview) {
        ProjectDetailVo.ProjectBaseInfo baseInfo = new ProjectDetailVo.ProjectBaseInfo();
        baseInfo.setProjectId(program.getId());
        baseInfo.setTitle(program.getTitle());
        baseInfo.setProjectTypeName(overview.getProjectTypeName());
        baseInfo.setParentProgramCategoryId(program.getParentProgramCategoryId());
        baseInfo.setParentProgramCategoryName(overview.getParentProgramCategoryName());
        baseInfo.setProgramCategoryId(program.getProgramCategoryId());
        baseInfo.setProgramCategoryName(overview.getProgramCategoryName());
        baseInfo.setAreaId(program.getAreaId());
        baseInfo.setActor(program.getActor());
        baseInfo.setMainActor(program.getMainActor());
        baseInfo.setPlace(program.getPlace());
        baseInfo.setItemPicture(program.getItemPicture());
        baseInfo.setDetail(program.getDetail());
        baseInfo.setProgramStatus(program.getProgramStatus());
        baseInfo.setProgramStatusName(overview.getProgramStatusName());
        baseInfo.setLifecycleStatus(overview.getLifecycleStatus());
        baseInfo.setLifecycleStatusName(overview.getLifecycleStatusName());
        baseInfo.setPreSell(program.getPreSell());
        baseInfo.setPreSellInstruction(program.getPreSellInstruction());
        baseInfo.setImportantNotice(program.getImportantNotice());
        baseInfo.setIssueTime(program.getIssueTime());
        baseInfo.setCreateTime(program.getCreateTime());
        baseInfo.setEditTime(program.getEditTime());
        return baseInfo;
    }

    private ProjectDetailVo.ContentInfo buildProjectContentInfo(Movie movie, MovieProfile profile,
                                                                List<MovieMedia> mediaList,
                                                                List<ProgramArtist> artistRelList) {
        ProjectDetailVo.ContentInfo content = new ProjectDetailVo.ContentInfo();
        if (Objects.nonNull(movie)) {
            content.setMovieId(movie.getId());
            content.setMovieName(movie.getMovieName());
            content.setMovieAlias(movie.getMovieAlias());
            content.setDirector(movie.getDirector());
            content.setActors(movie.getActors());
            content.setDurationMinutes(movie.getDurationMinutes());
            content.setLanguage(movie.getLanguage());
            content.setRegion(movie.getRegion());
            content.setReleaseDate(movie.getReleaseDate());
            content.setPoster(movie.getPoster());
        }
        if (Objects.nonNull(profile)) {
            content.setGenre(profile.getGenre());
            content.setReleaseStatus(profile.getReleaseStatus());
            content.setReleaseStatusName(releaseStatusName(profile.getReleaseStatus()));
            content.setWantWatchCount(profile.getWantWatchCount());
            content.setWatchedCount(profile.getWatchedCount());
            content.setRatingScore(profile.getRatingScore());
            content.setBoxOfficeAmount(profile.getBoxOfficeAmount());
            content.setProducer(profile.getProducer());
            content.setDistributor(profile.getDistributor());
            content.setAgeTips(profile.getAgeTips());
        }
        content.setArtistCount((long) artistRelList.size());
        content.setMediaCount((long) mediaList.size());
        content.setTrailerCount(countMovieMedia(mediaList, 1));
        content.setStillCount(countMovieMedia(mediaList, 2));
        content.setPosterCount(countMovieMedia(mediaList, 3));
        content.setArtistList(buildProjectArtistItemList(artistRelList));
        return content;
    }

    private ProjectDetailVo.TicketInfo buildProjectTicketInfo(Program program, List<TicketCategory> ticketList) {
        ProjectDetailVo.TicketInfo ticketInfo = new ProjectDetailVo.TicketInfo();
        ticketInfo.setTicketCategoryCount((long) ticketList.size());
        ticketInfo.setTotalNumber(sumLong(ticketList.stream().map(TicketCategory::getTotalNumber).toList()));
        ticketInfo.setTotalRemainNumber(sumLong(ticketList.stream().map(TicketCategory::getRemainNumber).toList()));
        ticketInfo.setMinPrice(ticketList.stream().map(TicketCategory::getPrice).filter(Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(null));
        ticketInfo.setMaxPrice(ticketList.stream().map(TicketCategory::getPrice).filter(Objects::nonNull)
                .max(BigDecimal::compareTo).orElse(null));
        ticketInfo.setPerOrderLimitPurchaseCount(program.getPerOrderLimitPurchaseCount());
        ticketInfo.setPerAccountLimitPurchaseCount(program.getPerAccountLimitPurchaseCount());
        ticketInfo.setPermitRefund(program.getPermitRefund());
        ticketInfo.setRelNameTicketEntrance(program.getRelNameTicketEntrance());
        ticketInfo.setPermitChooseSeat(program.getPermitChooseSeat());
        ticketInfo.setElectronicDeliveryTicket(program.getElectronicDeliveryTicket());
        ticketInfo.setElectronicInvoice(program.getElectronicInvoice());
        ticketInfo.setRefundTicketRule(program.getRefundTicketRule());
        ticketInfo.setRefundExplain(program.getRefundExplain());
        ticketInfo.setDeliveryInstruction(program.getDeliveryInstruction());
        ticketInfo.setEntryRule(program.getEntryRule());
        ticketInfo.setRealTicketPurchaseRule(program.getRealTicketPurchaseRule());
        ticketInfo.setRelNameTicketEntranceExplain(program.getRelNameTicketEntranceExplain());
        ticketInfo.setChooseSeatExplain(program.getChooseSeatExplain());
        ticketInfo.setElectronicDeliveryTicketExplain(program.getElectronicDeliveryTicketExplain());
        ticketInfo.setElectronicInvoiceExplain(program.getElectronicInvoiceExplain());
        ticketInfo.setTicketList(ticketList.stream().map(ticket -> {
            ProjectDetailVo.TicketItem item = new ProjectDetailVo.TicketItem();
            item.setTicketCategoryId(ticket.getId());
            item.setIntroduce(ticket.getIntroduce());
            item.setPrice(ticket.getPrice());
            item.setTotalNumber(ticket.getTotalNumber());
            item.setRemainNumber(ticket.getRemainNumber());
            return item;
        }).toList());
        return ticketInfo;
    }

    private ProjectDetailVo.ScreeningInfo buildProjectScreeningInfo(Long programId, ProjectManageVo overview) {
        ProjectDetailVo.ScreeningInfo info = new ProjectDetailVo.ScreeningInfo();
        info.setScreeningCount(overview.getScreeningCount());
        info.setActiveScreeningCount(overview.getActiveScreeningCount());
        info.setOpenInventoryIssueCount(overview.getOpenInventoryIssueCount());
        info.setCinemaCount(countDistinctMovieScreening(programId, "cinema_id"));
        info.setHallCount(countDistinctMovieScreening(programId, "hall_id"));
        MovieScreening first = selectEdgeMovieScreening(programId, true);
        MovieScreening last = selectEdgeMovieScreening(programId, false);
        if (Objects.nonNull(first)) {
            info.setFirstShowTime(first.getShowTime());
        }
        if (Objects.nonNull(last)) {
            info.setLastShowTime(last.getShowTime());
        }
        List<MovieScreening> upcomingList = selectUpcomingMovieScreeningList(programId);
        if (CollectionUtil.isNotEmpty(upcomingList)) {
            info.setNextShowTime(upcomingList.get(0).getShowTime());
        }
        info.setUpcomingScreeningList(buildScreeningPreviewList(upcomingList));
        return info;
    }

    private ProjectDetailVo.ComplianceInfo buildProjectComplianceInfo(Program program) {
        ProjectDetailVo.ComplianceInfo info = new ProjectDetailVo.ComplianceInfo();
        info.setRefundTicketRule(program.getRefundTicketRule());
        info.setRefundExplain(program.getRefundExplain());
        info.setDeliveryInstruction(program.getDeliveryInstruction());
        info.setEntryRule(program.getEntryRule());
        info.setRealTicketPurchaseRule(program.getRealTicketPurchaseRule());
        info.setChildPurchase(program.getChildPurchase());
        info.setInvoiceSpecification(program.getInvoiceSpecification());
        info.setAbnormalOrderDescription(program.getAbnormalOrderDescription());
        info.setKindReminder(program.getKindReminder());
        info.setProhibitedItem(program.getProhibitedItem());
        info.setDepositSpecification(program.getDepositSpecification());
        info.setPerformanceDuration(program.getPerformanceDuration());
        info.setEntryTime(program.getEntryTime());
        info.setRelNameTicketEntrance(program.getRelNameTicketEntrance());
        info.setRelNameTicketEntranceExplain(program.getRelNameTicketEntranceExplain());
        info.setPermitChooseSeat(program.getPermitChooseSeat());
        info.setChooseSeatExplain(program.getChooseSeatExplain());
        info.setElectronicDeliveryTicket(program.getElectronicDeliveryTicket());
        info.setElectronicDeliveryTicketExplain(program.getElectronicDeliveryTicketExplain());
        info.setElectronicInvoice(program.getElectronicInvoice());
        info.setElectronicInvoiceExplain(program.getElectronicInvoiceExplain());
        return info;
    }

    private List<ProjectDetailVo.ChecklistItem> buildProjectChecklist(Program program, ProjectManageVo overview,
                                                                      Movie movie, MovieProfile profile,
                                                                      List<TicketCategory> ticketList,
                                                                      List<MovieMedia> mediaList,
                                                                      List<ProgramArtist> artistRelList) {
        List<ProjectDetailVo.ChecklistItem> list = new ArrayList<>();
        addChecklist(list, "基础资料", "项目名称", StringUtil.isNotEmpty(program.getTitle()),
                "专业平台建项必须先有可识别项目名", "补齐项目名称");
        addChecklist(list, "基础资料", "主视觉/海报",
                StringUtil.isNotEmpty(program.getItemPicture()) || (Objects.nonNull(movie) && StringUtil.isNotEmpty(movie.getPoster())),
                "项目列表、详情页和分享卡片都需要稳定主视觉", "上传主图或影片海报");
        addChecklist(list, "内容资料", "详情介绍",
                StringUtil.isNotEmpty(program.getDetail()) || (Objects.nonNull(movie) && StringUtil.isNotEmpty(movie.getDescription())) ||
                        (Objects.nonNull(profile) && StringUtil.isNotEmpty(profile.getLongDescription())),
                "猫眼/淘票票/潮声类详情页必须有影片资料或演出详情", "补齐详情介绍");
        addChecklist(list, "内容资料", "主创/演员", CollectionUtil.isNotEmpty(artistRelList) ||
                StringUtil.isNotEmpty(program.getActor()) || StringUtil.isNotEmpty(program.getMainActor()) ||
                (Objects.nonNull(movie) && (StringUtil.isNotEmpty(movie.getDirector()) || StringUtil.isNotEmpty(movie.getActors()))),
                "专业平台会结构化展示导演、演员、嘉宾或艺人阵容", "维护演职员关系");
        addChecklist(list, "内容资料", "媒体物料", !"电影".equals(overview.getProjectTypeName()) || CollectionUtil.isNotEmpty(mediaList),
                "电影项目应能管理预告片、剧照、海报等物料", "上传预告片、剧照或海报");
        addChecklist(list, "票务库存", "票档/票版", CollectionUtil.isNotEmpty(ticketList),
                "售卖前必须有可审计票档、价格和库存", "配置票档和价格");
        addChecklist(list, "排期场地", "排期/场次", Optional.ofNullable(overview.getScreeningCount()).orElse(0L) > 0,
                "电影按影院/影厅/场次售卖，演出按场馆/场次售卖", "创建排期或场次");
        addChecklist(list, "售后履约", "退票规则", StringUtil.isNotEmpty(program.getRefundTicketRule()),
                "专业平台售卖页必须提前展示退改规则", "维护退票/换票规则");
        addChecklist(list, "售后履约", "入场规则", StringUtil.isNotEmpty(program.getEntryRule()),
                "核销、实名、儿童票和入场须知需要前后台一致", "维护入场规则");
        addChecklist(list, "场地主体", "影院/场馆", "电影".equals(overview.getProjectTypeName()) ?
                        Optional.ofNullable(overview.getScreeningCount()).orElse(0L) > 0 : StringUtil.isNotEmpty(program.getPlace()),
                "专业平台会围绕影院或场馆组织购票入口", "绑定影院、影厅或场馆");
        return list;
    }

    private List<ProjectDetailVo.RiskItem> buildProjectRiskItems(Program program, ProjectManageVo overview, Movie movie,
                                                                 MovieProfile profile, List<TicketCategory> ticketList,
                                                                 List<MovieMedia> mediaList,
                                                                 List<ProgramArtist> artistRelList) {
        List<ProjectDetailVo.RiskItem> list = new ArrayList<>();
        if (Optional.ofNullable(overview.getCompletenessScore()).orElse(0) < 80) {
            addRiskItem(list, "CONTENT_INCOMPLETE", "HIGH", "项目资料完整度低于 80%，前台展示和审核都会受影响", "先处理详情页检查清单中的未完成项");
        }
        if (CollectionUtil.isEmpty(ticketList)) {
            addRiskItem(list, "NO_TICKET_CATEGORY", "HIGH", "项目没有有效票档，无法形成可售库存", "进入票务库存中心配置票档/票版");
        }
        if (Optional.ofNullable(overview.getActiveScreeningCount()).orElse(0L) == 0 && "电影".equals(overview.getProjectTypeName())) {
            addRiskItem(list, "NO_ACTIVE_SCREENING", "HIGH", "电影项目没有未来有效场次，用户无法按影院购票", "进入排期中心创建或上架场次");
        }
        if (Optional.ofNullable(overview.getOpenInventoryIssueCount()).orElse(0L) > 0) {
            addRiskItem(list, "INVENTORY_RECONCILE", "HIGH", "存在未处理库存对账异常，可能导致可售状态不一致", "进入库存异常工单处理差异");
        }
        if (Objects.equals(program.getProgramStatus(), BusinessStatus.NO.getCode())) {
            addRiskItem(list, "OFF_SALE", "MEDIUM", "项目当前处于下架状态，前台不可购买", "确认资料、排期、库存无误后再执行上架");
        }
        if (Objects.nonNull(movie) && Objects.isNull(profile)) {
            addRiskItem(list, "MOVIE_PROFILE_MISSING", "MEDIUM", "电影缺少上映状态、类型、评分、票房等扩展资料", "进入内容中心补齐影片扩展资料");
        }
        if (Objects.nonNull(movie) && CollectionUtil.isEmpty(mediaList)) {
            addRiskItem(list, "MEDIA_MISSING", "MEDIUM", "电影缺少预告片、剧照、海报等媒体物料", "进入内容中心维护媒体资料");
        }
        if (Objects.nonNull(movie) && CollectionUtil.isEmpty(artistRelList) &&
                !StringUtil.isNotEmpty(movie.getDirector()) && !StringUtil.isNotEmpty(movie.getActors())) {
            addRiskItem(list, "ARTIST_MISSING", "MEDIUM", "电影缺少导演、演员等结构化主创信息", "维护节目影人关系");
        }
        return list;
    }

    private List<ProjectDetailVo.OperationAction> buildProjectOperationActions(ProjectManageVo overview) {
        List<ProjectDetailVo.OperationAction> list = new ArrayList<>();
        int sortOrder = 1;
        if (Optional.ofNullable(overview.getCompletenessScore()).orElse(0) < 80) {
            addOperationAction(list, "CONTENT", "补齐项目资料", "项目中心/内容中心", "补齐基础资料、主视觉、详情介绍、主创和售后规则", sortOrder++);
        }
        if (Optional.ofNullable(overview.getTicketCategoryCount()).orElse(0L) == 0) {
            addOperationAction(list, "TICKET", "配置票档/票版", "票务库存中心", "配置价格、库存、实名和售后规则后再进入售卖", sortOrder++);
        }
        if ("电影".equals(overview.getProjectTypeName()) && Optional.ofNullable(overview.getActiveScreeningCount()).orElse(0L) == 0) {
            addOperationAction(list, "SCHEDULE", "创建或上架排期", "排期中心", "按城市、影院、影厅、版本、语言和放映时间创建未来场次", sortOrder++);
        }
        if (Optional.ofNullable(overview.getOpenInventoryIssueCount()).orElse(0L) > 0) {
            addOperationAction(list, "RISK", "处理库存异常", "票务库存中心/风控中心", "处理 DB、Redis、座位状态和订单状态的未闭环异常", sortOrder++);
        }
        addOperationAction(list, "DATA", "监控销售与履约", "数据中心/履约中心", "持续关注销售、余票、退票、核销和异常趋势", sortOrder);
        return list;
    }

    private ProjectLifecycleVo buildProjectLifecycleVo(ProjectLifecycle lifecycle) {
        ProjectLifecycleVo vo = new ProjectLifecycleVo();
        BeanUtil.copyProperties(lifecycle, vo);
        vo.setLifecycleStatusName(ProjectLifecycleStatus.getMsg(lifecycle.getLifecycleStatus()));
        vo.setPreviousLifecycleStatusName(ProjectLifecycleStatus.getMsg(lifecycle.getPreviousLifecycleStatus()));
        vo.setReviewStatusName(reviewStatusName(lifecycle.getReviewStatus()));
        vo.setActionList(buildSupportedLifecycleActionList(lifecycle.getLifecycleStatus()));
        return vo;
    }

    private List<ProjectLifecycleRecordVo> selectProjectLifecycleRecordList(Long programId) {
        return projectLifecycleRecordMapper.selectList(Wrappers.lambdaQuery(ProjectLifecycleRecord.class)
                        .eq(ProjectLifecycleRecord::getProgramId, programId)
                        .eq(ProjectLifecycleRecord::getStatus, BusinessStatus.YES.getCode())
                        .orderByDesc(ProjectLifecycleRecord::getCreateTime)
                        .last("limit 20"))
                .stream().map(record -> {
                    ProjectLifecycleRecordVo vo = new ProjectLifecycleRecordVo();
                    BeanUtil.copyProperties(record, vo);
                    vo.setFromLifecycleStatusName(ProjectLifecycleStatus.getMsg(record.getFromLifecycleStatus()));
                    vo.setToLifecycleStatusName(ProjectLifecycleStatus.getMsg(record.getToLifecycleStatus()));
                    return vo;
                }).toList();
    }

    private List<ProjectLifecycleVo.LifecycleActionVo> buildSupportedLifecycleActionList(Integer currentStatus) {
        return lifecycleActionList().stream().filter(action -> canTransition(currentStatus, action)).map(action -> {
            ProjectLifecycleVo.LifecycleActionVo vo = new ProjectLifecycleVo.LifecycleActionVo();
            vo.setActionType(action.actionType());
            vo.setActionName(action.actionName());
            vo.setTargetLifecycleStatus(action.targetStatus());
            vo.setTargetLifecycleStatusName(ProjectLifecycleStatus.getMsg(action.targetStatus()));
            vo.setSortOrder(action.sortOrder());
            vo.setDangerFlag(action.dangerFlag() ? BusinessStatus.YES.getCode() : BusinessStatus.NO.getCode());
            vo.setConfirmText(action.dangerFlag() ? expectedConfirmText(action) : null);
            vo.setRiskTip(action.dangerFlag() ? lifecycleActionRiskTip(action) : null);
            return vo;
        }).toList();
    }

    private ProjectLifecycle ensureProjectLifecycle(Program program, ProjectManageVo overview) {
        ProjectLifecycle lifecycle = selectProjectLifecycle(program.getId());
        if (Objects.nonNull(lifecycle)) {
            return lifecycle;
        }
        Date now = DateUtils.now();
        lifecycle = new ProjectLifecycle();
        lifecycle.setId(uidGenerator.getUid());
        lifecycle.setProgramId(program.getId());
        lifecycle.setProjectTypeName(overview.getProjectTypeName());
        lifecycle.setLifecycleStatus(overview.getLifecycleStatus());
        lifecycle.setReviewStatus(PROJECT_REVIEW_NONE);
        lifecycle.setLastActionType("INIT");
        lifecycle.setLastActionName("初始化生命周期");
        lifecycle.setLastTransitionTime(now);
        lifecycle.setVersion(0);
        lifecycle.setCreateTime(now);
        lifecycle.setEditTime(now);
        lifecycle.setStatus(BusinessStatus.YES.getCode());
        projectLifecycleMapper.insert(lifecycle);
        insertProjectLifecycleRecord(lifecycle, null,
                new LifecycleAction("INIT", "初始化生命周期", overview.getLifecycleStatus(), 0, false), null, null);
        return lifecycle;
    }

    private Program selectProjectProgram(Long projectId) {
        Program program = programMapper.selectOne(Wrappers.lambdaQuery(Program.class)
                .eq(Program::getId, projectId)
                .eq(Program::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(program)) {
            throw new TidesFrameException(BaseCode.PROGRAM_NOT_EXIST);
        }
        return program;
    }

    private Map<Long, ProgramCategory> selectProgramCategoryMap(Set<Long> categoryIds) {
        if (CollectionUtil.isEmpty(categoryIds)) {
            return new HashMap<>(0);
        }
        return programCategoryMapper.selectList(Wrappers.lambdaQuery(ProgramCategory.class)
                        .in(ProgramCategory::getId, categoryIds)
                        .eq(ProgramCategory::getStatus, BusinessStatus.YES.getCode()))
                .stream().collect(Collectors.toMap(ProgramCategory::getId, item -> item, (oldValue, newValue) -> oldValue));
    }

    private Set<Long> collectCategoryIds(List<Program> programs) {
        Set<Long> ids = new HashSet<>();
        for (Program program : programs) {
            if (Objects.nonNull(program.getParentProgramCategoryId())) {
                ids.add(program.getParentProgramCategoryId());
            }
            if (Objects.nonNull(program.getProgramCategoryId())) {
                ids.add(program.getProgramCategoryId());
            }
        }
        return ids;
    }

    private List<TicketCategory> selectActiveTicketCategoryList(Long programId) {
        return ticketCategoryMapper.selectList(Wrappers.lambdaQuery(TicketCategory.class)
                .eq(TicketCategory::getProgramId, programId)
                .eq(TicketCategory::getStatus, BusinessStatus.YES.getCode())
                .orderByAsc(TicketCategory::getPrice));
    }

    private List<MovieMedia> selectActiveMovieMediaList(Long movieId) {
        return movieMediaMapper.selectList(Wrappers.lambdaQuery(MovieMedia.class)
                .eq(MovieMedia::getMovieId, movieId)
                .eq(MovieMedia::getStatus, BusinessStatus.YES.getCode())
                .orderByAsc(MovieMedia::getSortOrder)
                .orderByDesc(MovieMedia::getEditTime));
    }

    private List<ProgramArtist> selectActiveProgramArtistList(Long programId) {
        return programArtistMapper.selectList(Wrappers.lambdaQuery(ProgramArtist.class)
                .eq(ProgramArtist::getProgramId, programId)
                .eq(ProgramArtist::getStatus, BusinessStatus.YES.getCode())
                .orderByAsc(ProgramArtist::getSortOrder)
                .orderByDesc(ProgramArtist::getEditTime)
                .last("limit 20"));
    }

    private ProjectLifecycle selectProjectLifecycle(Long programId) {
        return projectLifecycleMapper.selectOne(Wrappers.lambdaQuery(ProjectLifecycle.class)
                .eq(ProjectLifecycle::getProgramId, programId)
                .eq(ProjectLifecycle::getStatus, BusinessStatus.YES.getCode())
                .last("limit 1"));
    }

    private Integer selectPersistedLifecycleStatus(Long programId) {
        ProjectLifecycle lifecycle = selectProjectLifecycle(programId);
        return Objects.isNull(lifecycle) ? null : lifecycle.getLifecycleStatus();
    }

    private Movie selectMovieByProgramId(Long programId) {
        return movieMapper.selectOne(Wrappers.lambdaQuery(Movie.class)
                .eq(Movie::getProgramId, programId)
                .eq(Movie::getStatus, BusinessStatus.YES.getCode())
                .last("limit 1"));
    }

    private MovieProfile selectMovieProfile(Long movieId, Long programId) {
        return movieProfileMapper.selectOne(Wrappers.lambdaQuery(MovieProfile.class)
                .eq(MovieProfile::getMovieId, movieId)
                .eq(MovieProfile::getProgramId, programId)
                .eq(MovieProfile::getStatus, BusinessStatus.YES.getCode())
                .last("limit 1"));
    }

    private String selectCategoryName(Map<Long, ProgramCategory> categoryMap, Long categoryId) {
        ProgramCategory category = categoryMap.get(categoryId);
        return Objects.isNull(category) ? null : category.getName();
    }

    private String resolveProjectTypeName(String parentCategoryName, String categoryName) {
        String text = Optional.ofNullable(parentCategoryName).orElse("") + Optional.ofNullable(categoryName).orElse("");
        if (text.contains("电影")) {
            return "电影";
        }
        if (text.contains("演唱") || text.contains("话剧") || text.contains("音乐") ||
                text.contains("戏剧") || text.contains("展览") || text.contains("体育")) {
            return "演出";
        }
        return "项目";
    }

    private Integer resolveProjectLifecycleStatus(Program program, MovieProfile profile) {
        if (Objects.equals(program.getProgramStatus(), BusinessStatus.NO.getCode())) {
            return ProjectLifecycleStatus.SALE_STOPPED.getCode();
        }
        if (Objects.nonNull(profile) && Objects.nonNull(profile.getReleaseStatus())) {
            return switch (profile.getReleaseStatus()) {
                case 0 -> ProjectLifecycleStatus.PENDING_SCHEDULE.getCode();
                case 1 -> ProjectLifecycleStatus.WARMING_UP.getCode();
                case 2 -> ProjectLifecycleStatus.ON_SALE.getCode();
                case 3 -> ProjectLifecycleStatus.FINISHED.getCode();
                case 4 -> ProjectLifecycleStatus.PRE_SELLING.getCode();
                default -> ProjectLifecycleStatus.DRAFT.getCode();
            };
        }
        if (Objects.equals(program.getPreSell(), BusinessStatus.YES.getCode())) {
            return ProjectLifecycleStatus.PRE_SELLING.getCode();
        }
        if (Objects.equals(program.getProgramStatus(), BusinessStatus.YES.getCode())) {
            return ProjectLifecycleStatus.ON_SALE.getCode();
        }
        return ProjectLifecycleStatus.DRAFT.getCode();
    }

    private void fillOverviewLifecycle(ProjectManageVo overview, ProjectLifecycle lifecycle) {
        overview.setLifecycleStatus(lifecycle.getLifecycleStatus());
        overview.setLifecycleStatusName(ProjectLifecycleStatus.getMsg(lifecycle.getLifecycleStatus()));
    }

    private int calculateProjectCompleteness(Program program, Movie movie, MovieProfile profile,
                                             Long ticketCategoryCount, Long screeningCount) {
        int completed = 0;
        completed += StringUtil.isNotEmpty(program.getTitle()) ? 1 : 0;
        completed += StringUtil.isNotEmpty(program.getItemPicture()) ? 1 : 0;
        completed += StringUtil.isNotEmpty(program.getDetail()) ? 1 : 0;
        completed += StringUtil.isNotEmpty(program.getRefundTicketRule()) ? 1 : 0;
        completed += StringUtil.isNotEmpty(program.getEntryRule()) ? 1 : 0;
        completed += Optional.ofNullable(ticketCategoryCount).orElse(0L) > 0 ? 1 : 0;
        completed += Optional.ofNullable(screeningCount).orElse(0L) > 0 ? 1 : 0;
        completed += Objects.nonNull(movie) ? (Objects.nonNull(profile) ? 1 : 0) : (StringUtil.isNotEmpty(program.getPlace()) ? 1 : 0);
        return completed * 100 / 8;
    }

    private int calculateProjectRiskCount(Integer completenessScore, ProjectManageVo vo) {
        int count = 0;
        if (Optional.ofNullable(completenessScore).orElse(0) < 80) {
            count++;
        }
        if (Optional.ofNullable(vo.getTicketCategoryCount()).orElse(0L) == 0) {
            count++;
        }
        if (Optional.ofNullable(vo.getActiveScreeningCount()).orElse(0L) == 0 &&
                ("电影".equals(vo.getProjectTypeName()) || Objects.equals(vo.getLifecycleStatus(), ProjectLifecycleStatus.ON_SALE.getCode()))) {
            count++;
        }
        if (Optional.ofNullable(vo.getOpenInventoryIssueCount()).orElse(0L) > 0) {
            count++;
        }
        return count;
    }

    private String resolveProjectNextAction(ProjectManageVo vo) {
        if (Optional.ofNullable(vo.getCompletenessScore()).orElse(0) < 80) {
            return "补齐项目资料";
        }
        if (Optional.ofNullable(vo.getTicketCategoryCount()).orElse(0L) == 0) {
            return "配置票档/票版";
        }
        if (Optional.ofNullable(vo.getActiveScreeningCount()).orElse(0L) == 0 && "电影".equals(vo.getProjectTypeName())) {
            return "创建或上架排片";
        }
        if (Optional.ofNullable(vo.getOpenInventoryIssueCount()).orElse(0L) > 0) {
            return "处理库存异常";
        }
        if (Objects.equals(vo.getLifecycleStatus(), ProjectLifecycleStatus.SALE_STOPPED.getCode())) {
            return "确认是否重新上架";
        }
        return "持续监控销售与履约";
    }

    private List<ProjectDetailVo.ArtistItem> buildProjectArtistItemList(List<ProgramArtist> artistRelList) {
        Map<Long, Artist> artistMap = selectArtistMapByIds(artistRelList.stream()
                .map(ProgramArtist::getArtistId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return artistRelList.stream().map(rel -> {
            ProjectDetailVo.ArtistItem item = new ProjectDetailVo.ArtistItem();
            item.setArtistId(rel.getArtistId());
            Artist artist = artistMap.get(rel.getArtistId());
            if (Objects.nonNull(artist)) {
                item.setArtistName(artist.getArtistName());
            }
            item.setRoleType(rel.getRoleType());
            item.setRoleName(rel.getRoleName());
            item.setDisplayFlag(rel.getDisplayFlag());
            item.setSortOrder(rel.getSortOrder());
            return item;
        }).toList();
    }

    private Map<Long, Artist> selectArtistMapByIds(Collection<Long> artistIds) {
        if (CollectionUtil.isEmpty(artistIds)) {
            return new HashMap<>();
        }
        return artistMapper.selectBatchIds(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, item -> item, (oldValue, newValue) -> oldValue));
    }

    private String releaseStatusName(Integer releaseStatus) {
        if (Objects.isNull(releaseStatus)) {
            return null;
        }
        return switch (releaseStatus) {
            case 0 -> "待定档";
            case 1 -> "即将上映";
            case 2 -> "正在热映";
            case 3 -> "已下映";
            case 4 -> "点映/预售";
            default -> "未知";
        };
    }

    private Long countMovieMedia(List<MovieMedia> mediaList, Integer mediaType) {
        return mediaList.stream().filter(item -> Objects.equals(item.getMediaType(), mediaType)).count();
    }

    private Long countDistinctMovieScreening(Long programId, String columnName) {
        List<Object> countList = movieScreeningMapper.selectObjs(new QueryWrapper<MovieScreening>()
                .select("count(distinct " + columnName + ")")
                .eq("program_id", programId)
                .eq("status", BusinessStatus.YES.getCode()));
        if (CollectionUtil.isEmpty(countList) || Objects.isNull(countList.get(0))) {
            return 0L;
        }
        Object value = countList.get(0);
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private MovieScreening selectEdgeMovieScreening(Long programId, boolean first) {
        return movieScreeningMapper.selectOne(Wrappers.lambdaQuery(MovieScreening.class)
                .eq(MovieScreening::getProgramId, programId)
                .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode())
                .orderBy(true, first, MovieScreening::getShowTime)
                .last("limit 1"));
    }

    private List<MovieScreening> selectUpcomingMovieScreeningList(Long programId) {
        return movieScreeningMapper.selectList(Wrappers.lambdaQuery(MovieScreening.class)
                .eq(MovieScreening::getProgramId, programId)
                .eq(MovieScreening::getScreeningStatus, BusinessStatus.YES.getCode())
                .eq(MovieScreening::getStatus, BusinessStatus.YES.getCode())
                .ge(MovieScreening::getShowTime, DateUtils.now())
                .orderByAsc(MovieScreening::getShowTime)
                .last("limit 6"));
    }

    private List<ProjectDetailVo.ScreeningPreview> buildScreeningPreviewList(List<MovieScreening> screeningList) {
        if (CollectionUtil.isEmpty(screeningList)) {
            return List.of();
        }
        Map<Long, Cinema> cinemaMap = selectCinemaMap(screeningList);
        Map<Long, Hall> hallMap = selectHallMap(screeningList);
        Map<Long, Long> remainMap = selectScreeningRemainNumberMap(screeningList.stream().map(MovieScreening::getId).toList());
        return screeningList.stream().map(screening -> {
            ProjectDetailVo.ScreeningPreview preview = new ProjectDetailVo.ScreeningPreview();
            preview.setScreeningId(screening.getId());
            preview.setCinemaId(screening.getCinemaId());
            Cinema cinema = cinemaMap.get(screening.getCinemaId());
            if (Objects.nonNull(cinema)) {
                preview.setCinemaName(cinema.getCinemaName());
                preview.setCityName(cinema.getCityName());
                preview.setDistrictName(cinema.getDistrictName());
                preview.setAddress(cinema.getAddress());
            }
            preview.setHallId(screening.getHallId());
            Hall hall = hallMap.get(screening.getHallId());
            if (Objects.nonNull(hall)) {
                preview.setHallName(hall.getHallName());
                preview.setHallType(hall.getHallType());
            }
            preview.setShowTime(screening.getShowTime());
            preview.setEndTime(screening.getEndTime());
            preview.setLanguage(screening.getLanguage());
            preview.setVersion(screening.getVersion());
            preview.setLowestPrice(screening.getLowestPrice());
            preview.setScreeningStatus(screening.getScreeningStatus());
            preview.setScreeningStatusName(Objects.equals(screening.getScreeningStatus(), BusinessStatus.YES.getCode()) ? "上架" : "下架");
            preview.setDbRemainNumber(Optional.ofNullable(remainMap.get(screening.getId())).orElse(0L));
            return preview;
        }).toList();
    }

    private Map<Long, Cinema> selectCinemaMap(List<MovieScreening> screeningList) {
        List<Long> ids = screeningList.stream().map(MovieScreening::getCinemaId).filter(Objects::nonNull).distinct().toList();
        if (CollectionUtil.isEmpty(ids)) {
            return new HashMap<>();
        }
        return cinemaMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Cinema::getId, item -> item, (oldValue, newValue) -> oldValue));
    }

    private Map<Long, Hall> selectHallMap(List<MovieScreening> screeningList) {
        List<Long> ids = screeningList.stream().map(MovieScreening::getHallId).filter(Objects::nonNull).distinct().toList();
        if (CollectionUtil.isEmpty(ids)) {
            return new HashMap<>();
        }
        return hallMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Hall::getId, item -> item, (oldValue, newValue) -> oldValue));
    }

    private Map<Long, Long> selectScreeningRemainNumberMap(List<Long> screeningIds) {
        if (CollectionUtil.isEmpty(screeningIds)) {
            return new HashMap<>();
        }
        List<MovieScreeningPrice> priceList = movieScreeningPriceMapper.selectList(Wrappers.lambdaQuery(MovieScreeningPrice.class)
                .in(MovieScreeningPrice::getScreeningId, screeningIds)
                .eq(MovieScreeningPrice::getStatus, BusinessStatus.YES.getCode()));
        if (CollectionUtil.isEmpty(priceList)) {
            return new HashMap<>();
        }
        return priceList.stream().collect(Collectors.groupingBy(MovieScreeningPrice::getScreeningId,
                Collectors.summingLong(price -> Optional.ofNullable(price.getRemainNumber()).orElse(0L))));
    }

    private List<LifecycleAction> lifecycleActionList() {
        return List.of(
                new LifecycleAction(ACTION_SUBMIT_REVIEW, "提交审核", ProjectLifecycleStatus.PENDING_REVIEW.getCode(), 1, false),
                new LifecycleAction(ACTION_APPROVE, "审核通过", ProjectLifecycleStatus.PENDING_SCHEDULE.getCode(), 2, false),
                new LifecycleAction(ACTION_REJECT, "审核驳回", ProjectLifecycleStatus.REVIEW_REJECTED.getCode(), 3, true),
                new LifecycleAction(ACTION_READY_WARMUP, "进入预热", ProjectLifecycleStatus.WARMING_UP.getCode(), 4, false),
                new LifecycleAction(ACTION_START_PRESELL, "开始预售", ProjectLifecycleStatus.PRE_SELLING.getCode(), 5, false),
                new LifecycleAction(ACTION_START_SELL, "上架售卖", ProjectLifecycleStatus.ON_SALE.getCode(), 6, false),
                new LifecycleAction(ACTION_STOP_SELL, "停售", ProjectLifecycleStatus.SALE_STOPPED.getCode(), 7, true),
                new LifecycleAction(ACTION_MARK_PERFORMING, "标记放映/演出中", ProjectLifecycleStatus.PERFORMING.getCode(), 8, false),
                new LifecycleAction(ACTION_FINISH, "标记结束", ProjectLifecycleStatus.FINISHED.getCode(), 9, false),
                new LifecycleAction(ACTION_SETTLE, "进入结算", ProjectLifecycleStatus.SETTLING.getCode(), 10, false),
                new LifecycleAction(ACTION_ARCHIVE, "归档", ProjectLifecycleStatus.ARCHIVED.getCode(), 11, false),
                new LifecycleAction(ACTION_POSTPONE, "延期", ProjectLifecycleStatus.POSTPONED.getCode(), 12, true),
                new LifecycleAction(ACTION_CANCEL, "取消", ProjectLifecycleStatus.CANCELED.getCode(), 13, true));
    }

    private LifecycleAction resolveLifecycleAction(String actionType) {
        return lifecycleActionList().stream().filter(action -> Objects.equals(action.actionType(), actionType)).findFirst().orElse(null);
    }

    private void validateLifecycleTransitionRisk(LifecycleAction action, ProjectLifecycleTransitionDto dto) {
        if (StringUtil.isNotEmpty(dto.getRemark()) && dto.getRemark().length() > LIFECYCLE_REMARK_MAX_LENGTH) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        if (!action.dangerFlag()) {
            return;
        }
        if (!Boolean.TRUE.equals(dto.getRiskAccepted())) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        if (!Objects.equals(expectedConfirmText(action), dto.getConfirmText())) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        if (!StringUtil.isNotEmpty(dto.getRemark()) || dto.getRemark().length() < LIFECYCLE_REMARK_MIN_LENGTH) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
    }

    private String expectedConfirmText(LifecycleAction action) {
        return "确认" + action.actionName();
    }

    private String lifecycleActionRiskTip(LifecycleAction action) {
        return switch (action.actionType()) {
            case ACTION_REJECT -> "会退回项目审核，项目需要重新补充资料后再提交。";
            case ACTION_STOP_SELL -> "会关闭前台售卖入口，影响购票、渠道同步、客服解释和余票监控。";
            case ACTION_POSTPONE -> "会影响已购订单履约，需要联动用户通知、退改规则、客服口径和结算排期。";
            case ACTION_CANCEL -> "会终止项目售卖和履约，需要联动退款、用户通知、客服口径和财务结算。";
            default -> "该动作会影响项目售卖或履约，请确认影响范围。";
        };
    }

    private String normalizeText(String value) {
        return Objects.isNull(value) ? null : value.trim();
    }

    private boolean canTransition(Integer currentStatus, LifecycleAction action) {
        if (Objects.isNull(currentStatus) || Objects.isNull(action) ||
                List.of(ProjectLifecycleStatus.ARCHIVED.getCode(), ProjectLifecycleStatus.CANCELED.getCode()).contains(currentStatus)) {
            return false;
        }
        return switch (action.actionType()) {
            case ACTION_SUBMIT_REVIEW -> List.of(ProjectLifecycleStatus.DRAFT.getCode(), ProjectLifecycleStatus.REVIEW_REJECTED.getCode(),
                    ProjectLifecycleStatus.SALE_STOPPED.getCode(), ProjectLifecycleStatus.POSTPONED.getCode()).contains(currentStatus);
            case ACTION_APPROVE, ACTION_REJECT -> Objects.equals(currentStatus, ProjectLifecycleStatus.PENDING_REVIEW.getCode());
            case ACTION_READY_WARMUP -> List.of(ProjectLifecycleStatus.PENDING_SCHEDULE.getCode(), ProjectLifecycleStatus.REVIEW_REJECTED.getCode()).contains(currentStatus);
            case ACTION_START_PRESELL -> List.of(ProjectLifecycleStatus.PENDING_SCHEDULE.getCode(), ProjectLifecycleStatus.WARMING_UP.getCode()).contains(currentStatus);
            case ACTION_START_SELL -> List.of(ProjectLifecycleStatus.PENDING_SCHEDULE.getCode(), ProjectLifecycleStatus.WARMING_UP.getCode(),
                    ProjectLifecycleStatus.PRE_SELLING.getCode(), ProjectLifecycleStatus.SALE_STOPPED.getCode()).contains(currentStatus);
            case ACTION_STOP_SELL -> List.of(ProjectLifecycleStatus.WARMING_UP.getCode(), ProjectLifecycleStatus.PRE_SELLING.getCode(),
                    ProjectLifecycleStatus.ON_SALE.getCode(), ProjectLifecycleStatus.PERFORMING.getCode()).contains(currentStatus);
            case ACTION_MARK_PERFORMING -> List.of(ProjectLifecycleStatus.ON_SALE.getCode(), ProjectLifecycleStatus.PRE_SELLING.getCode()).contains(currentStatus);
            case ACTION_FINISH -> List.of(ProjectLifecycleStatus.PERFORMING.getCode(), ProjectLifecycleStatus.ON_SALE.getCode()).contains(currentStatus);
            case ACTION_SETTLE -> Objects.equals(currentStatus, ProjectLifecycleStatus.FINISHED.getCode());
            case ACTION_ARCHIVE -> List.of(ProjectLifecycleStatus.FINISHED.getCode(), ProjectLifecycleStatus.SETTLING.getCode(),
                    ProjectLifecycleStatus.SALE_STOPPED.getCode(), ProjectLifecycleStatus.POSTPONED.getCode()).contains(currentStatus);
            case ACTION_POSTPONE, ACTION_CANCEL -> !List.of(ProjectLifecycleStatus.SETTLING.getCode(), ProjectLifecycleStatus.ARCHIVED.getCode(),
                    ProjectLifecycleStatus.CANCELED.getCode()).contains(currentStatus);
            default -> false;
        };
    }

    private Integer resolveReviewStatus(LifecycleAction action) {
        return switch (action.actionType()) {
            case ACTION_SUBMIT_REVIEW -> PROJECT_REVIEW_PENDING;
            case ACTION_APPROVE -> PROJECT_REVIEW_APPROVED;
            case ACTION_REJECT -> PROJECT_REVIEW_REJECTED;
            default -> PROJECT_REVIEW_NONE;
        };
    }

    private String reviewStatusName(Integer reviewStatus) {
        if (Objects.isNull(reviewStatus)) {
            return "无";
        }
        return switch (reviewStatus) {
            case PROJECT_REVIEW_PENDING -> "待审核";
            case PROJECT_REVIEW_APPROVED -> "审核通过";
            case PROJECT_REVIEW_REJECTED -> "审核驳回";
            default -> "无";
        };
    }

    private void updateProgramStatusByLifecycle(Program program, Integer targetStatus) {
        Program update = new Program();
        update.setEditTime(DateUtils.now());
        if (List.of(ProjectLifecycleStatus.WARMING_UP.getCode(), ProjectLifecycleStatus.PRE_SELLING.getCode(),
                ProjectLifecycleStatus.ON_SALE.getCode(), ProjectLifecycleStatus.PERFORMING.getCode()).contains(targetStatus)) {
            update.setProgramStatus(BusinessStatus.YES.getCode());
            if (Objects.isNull(program.getIssueTime())) {
                update.setIssueTime(DateUtils.now());
            }
        } else {
            update.setProgramStatus(BusinessStatus.NO.getCode());
        }
        programMapper.update(update, Wrappers.lambdaUpdate(Program.class)
                .eq(Program::getId, program.getId())
                .eq(Program::getStatus, BusinessStatus.YES.getCode()));
    }

    private void insertProjectLifecycleRecord(ProjectLifecycle lifecycle, Integer fromStatus, LifecycleAction action,
                                              ProjectLifecycleTransitionDto transitionDto,
                                              BackManageOperatorContextService.OperatorContext operatorContext) {
        ProjectLifecycleRecord record = new ProjectLifecycleRecord();
        record.setId(uidGenerator.getUid());
        record.setLifecycleId(lifecycle.getId());
        record.setProgramId(lifecycle.getProgramId());
        record.setFromLifecycleStatus(fromStatus);
        record.setToLifecycleStatus(action.targetStatus());
        record.setActionType(action.actionType());
        record.setActionName(action.actionName());
        if (Objects.nonNull(transitionDto)) {
            record.setOperatorId(Objects.isNull(operatorContext) ? transitionDto.getOperatorId() :
                    operatorContext.operatorId());
            record.setOperatorName(Objects.isNull(operatorContext) ? transitionDto.getOperatorName() :
                    operatorContext.operatorName());
            record.setRemark(transitionDto.getRemark());
        }
        record.setSnapshotJson(buildLifecycleSnapshotJson(lifecycle, fromStatus, action, transitionDto));
        record.setCreateTime(DateUtils.now());
        record.setEditTime(DateUtils.now());
        record.setStatus(BusinessStatus.YES.getCode());
        projectLifecycleRecordMapper.insert(record);
    }

    private String buildLifecycleSnapshotJson(ProjectLifecycle lifecycle, Integer fromStatus, LifecycleAction action,
                                              ProjectLifecycleTransitionDto transitionDto) {
        return "{" +
                "\"programId\":" + lifecycle.getProgramId() + "," +
                "\"fromStatus\":" + Optional.ofNullable(fromStatus).map(String::valueOf).orElse("null") + "," +
                "\"toStatus\":" + action.targetStatus() + "," +
                "\"actionType\":\"" + action.actionType() + "\"," +
                "\"dangerFlag\":" + action.dangerFlag() + "," +
                "\"riskAccepted\":" + (Objects.nonNull(transitionDto) && Boolean.TRUE.equals(transitionDto.getRiskAccepted())) + "," +
                "\"version\":" + Optional.ofNullable(lifecycle.getVersion()).orElse(0) +
                "}";
    }

    private Long sumLong(List<Long> values) {
        long sum = 0L;
        for (Long value : values) {
            sum += Optional.ofNullable(value).orElse(0L);
        }
        return sum;
    }

    private void addChecklist(List<ProjectDetailVo.ChecklistItem> list, String groupName, String itemName,
                              Boolean completed, String professionalBenchmark, String advice) {
        ProjectDetailVo.ChecklistItem item = new ProjectDetailVo.ChecklistItem();
        item.setGroupName(groupName);
        item.setItemName(itemName);
        item.setCompleted(completed);
        item.setProfessionalBenchmark(professionalBenchmark);
        item.setAdvice(completed ? "已满足" : advice);
        list.add(item);
    }

    private void addRiskItem(List<ProjectDetailVo.RiskItem> list, String riskType, String riskLevel,
                             String problemDesc, String nextAction) {
        ProjectDetailVo.RiskItem item = new ProjectDetailVo.RiskItem();
        item.setRiskType(riskType);
        item.setRiskLevel(riskLevel);
        item.setProblemDesc(problemDesc);
        item.setNextAction(nextAction);
        list.add(item);
    }

    private void addOperationAction(List<ProjectDetailVo.OperationAction> list, String actionType,
                                    String actionName, String targetCenter, String description, Integer sortOrder) {
        ProjectDetailVo.OperationAction item = new ProjectDetailVo.OperationAction();
        item.setActionType(actionType);
        item.setActionName(actionName);
        item.setTargetCenter(targetCenter);
        item.setDescription(description);
        item.setSortOrder(sortOrder);
        list.add(item);
    }

    private record LifecycleAction(String actionType, String actionName, Integer targetStatus, Integer sortOrder,
                                   boolean dangerFlag) {
    }
}
