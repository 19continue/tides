package com.tides.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baidu.fsg.uid.UidGenerator;
import com.tides.dto.ProjectApprovalHandleDto;
import com.tides.dto.ProjectApprovalManageDto;
import com.tides.dto.ProjectLifecycleTransitionDto;
import com.tides.entity.Program;
import com.tides.entity.ProjectApprovalOrder;
import com.tides.entity.ProjectLifecycle;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.enums.ProjectLifecycleStatus;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.ProjectApprovalOrderMapper;
import com.tides.page.PageUtil;
import com.tides.util.DateUtils;
import com.tides.util.StringUtil;
import com.tides.vo.ProjectApprovalOrderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectApprovalService {

    public static final int APPROVAL_PENDING = 1;
    public static final int APPROVAL_APPROVED = 2;
    public static final int APPROVAL_REJECTED = 3;

    private static final String APPROVAL_TYPE_LIFECYCLE = "LIFECYCLE";
    private static final String RISK_HIGH = "HIGH";

    @Autowired
    private UidGenerator uidGenerator;
    @Autowired
    private ProjectApprovalOrderMapper projectApprovalOrderMapper;
    @Autowired
    private ProjectAuditLogService projectAuditLogService;
    @Autowired
    private ProjectApprovalPolicyService projectApprovalPolicyService;

    public ProjectApprovalOrder createLifecycleApproval(Program program, ProjectLifecycle lifecycle,
                                                        ProjectLifecycleTransitionDto dto,
                                                        String actionName, Integer targetLifecycleStatus,
                                                        BackManageOperatorContextService.OperatorContext operatorContext) {
        if (existsPendingLifecycleApproval(program.getId(), dto.getActionType())) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        Date now = DateUtils.now();
        ProjectApprovalOrder approvalOrder = new ProjectApprovalOrder();
        approvalOrder.setId(uidGenerator.getUid());
        approvalOrder.setProgramId(program.getId());
        approvalOrder.setApprovalType(APPROVAL_TYPE_LIFECYCLE);
        approvalOrder.setApprovalTitle(program.getTitle() + " - " + actionName);
        approvalOrder.setActionType(dto.getActionType());
        approvalOrder.setActionName(actionName);
        approvalOrder.setFromLifecycleStatus(lifecycle.getLifecycleStatus());
        approvalOrder.setTargetLifecycleStatus(targetLifecycleStatus);
        approvalOrder.setApprovalStatus(APPROVAL_PENDING);
        approvalOrder.setRiskLevel(RISK_HIGH);
        approvalOrder.setRequesterId(operatorContext.operatorId());
        approvalOrder.setRequesterName(operatorContext.operatorName());
        approvalOrder.setRequesterAccount(operatorContext.operatorAccount());
        approvalOrder.setRequestReason(dto.getRemark());
        approvalOrder.setConfirmText(dto.getConfirmText());
        approvalOrder.setApplyTime(now);
        approvalOrder.setSnapshotJson(buildLifecycleApprovalSnapshot(program, lifecycle, dto));
        approvalOrder.setCreateTime(now);
        approvalOrder.setEditTime(now);
        approvalOrder.setStatus(BusinessStatus.YES.getCode());
        projectApprovalOrderMapper.insert(approvalOrder);
        projectAuditLogService.recordApprovalSubmit(program.getId(), operatorContext.operatorId(),
                operatorContext.operatorName(), "提交高危审批：" + actionName, dto.getRemark(), approvalOrder);
        return approvalOrder;
    }

    public ProjectApprovalOrder handleApproval(ProjectApprovalHandleDto dto,
                                               BackManageOperatorContextService.OperatorContext operatorContext) {
        if (!List.of(APPROVAL_APPROVED, APPROVAL_REJECTED).contains(dto.getApproveResult())) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        ProjectApprovalOrder before = selectPendingApproval(dto.getApprovalId(), dto.getProgramId());
        validateNotSelfApproval(before, operatorContext);
        projectApprovalPolicyService.validateApprovalPermission(before.getActionType(), operatorContext);
        Date now = DateUtils.now();
        ProjectApprovalOrder update = new ProjectApprovalOrder();
        update.setApprovalStatus(dto.getApproveResult());
        update.setApproverId(operatorContext.operatorId());
        update.setApproverName(operatorContext.operatorName());
        update.setApproverAccount(operatorContext.operatorAccount());
        update.setApproveRemark(normalizeText(dto.getApproveRemark()));
        update.setApproveTime(now);
        update.setEditTime(now);
        if (Objects.equals(dto.getApproveResult(), APPROVAL_APPROVED)) {
            update.setExecuteTime(now);
        }
        int updateCount = projectApprovalOrderMapper.update(update, Wrappers.lambdaUpdate(ProjectApprovalOrder.class)
                .eq(ProjectApprovalOrder::getId, dto.getApprovalId())
                .eq(ProjectApprovalOrder::getProgramId, dto.getProgramId())
                .eq(ProjectApprovalOrder::getApprovalStatus, APPROVAL_PENDING)
                .eq(ProjectApprovalOrder::getStatus, BusinessStatus.YES.getCode()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        ProjectApprovalOrder after = projectApprovalOrderMapper.selectOne(Wrappers.lambdaQuery(ProjectApprovalOrder.class)
                .eq(ProjectApprovalOrder::getId, dto.getApprovalId())
                .eq(ProjectApprovalOrder::getProgramId, dto.getProgramId())
                .eq(ProjectApprovalOrder::getStatus, BusinessStatus.YES.getCode()));
        projectAuditLogService.recordApprovalHandle(after.getProgramId(), operatorContext.operatorId(),
                operatorContext.operatorName(), approvalStatusName(after.getApprovalStatus()), after.getApproveRemark(),
                before, after);
        return after;
    }

    public IPage<ProjectApprovalOrderVo> projectApprovalPage(ProjectApprovalManageDto dto) {
        Date createTimeStart = DateUtils.parseDate(dto.getCreateTimeStart());
        Date createTimeEnd = DateUtils.parseDate(dto.getCreateTimeEnd());
        LambdaQueryWrapper<ProjectApprovalOrder> queryWrapper = Wrappers.lambdaQuery(ProjectApprovalOrder.class)
                .eq(Objects.nonNull(dto.getProgramId()), ProjectApprovalOrder::getProgramId, dto.getProgramId())
                .eq(Objects.nonNull(dto.getApprovalStatus()), ProjectApprovalOrder::getApprovalStatus,
                        dto.getApprovalStatus())
                .eq(StringUtil.isNotEmpty(dto.getActionType()), ProjectApprovalOrder::getActionType,
                        dto.getActionType())
                .like(StringUtil.isNotEmpty(dto.getRequesterName()), ProjectApprovalOrder::getRequesterName,
                        dto.getRequesterName())
                .like(StringUtil.isNotEmpty(dto.getApproverName()), ProjectApprovalOrder::getApproverName,
                        dto.getApproverName())
                .ge(Objects.nonNull(createTimeStart), ProjectApprovalOrder::getCreateTime, createTimeStart)
                .le(Objects.nonNull(createTimeEnd), ProjectApprovalOrder::getCreateTime, createTimeEnd)
                .eq(ProjectApprovalOrder::getStatus, BusinessStatus.YES.getCode())
                .orderByDesc(ProjectApprovalOrder::getCreateTime)
                .orderByDesc(ProjectApprovalOrder::getId);
        IPage<ProjectApprovalOrder> page = projectApprovalOrderMapper.selectPage(PageUtil.getPageParams(dto),
                queryWrapper);
        IPage<ProjectApprovalOrderVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(buildApprovalOrderVoList(page.getRecords()));
        return result;
    }

    public List<ProjectApprovalOrderVo> selectRecentApprovalOrderList(Long programId) {
        return buildApprovalOrderVoList(projectApprovalOrderMapper.selectList(Wrappers.lambdaQuery(ProjectApprovalOrder.class)
                .eq(ProjectApprovalOrder::getProgramId, programId)
                .eq(ProjectApprovalOrder::getStatus, BusinessStatus.YES.getCode())
                .orderByDesc(ProjectApprovalOrder::getCreateTime)
                .orderByDesc(ProjectApprovalOrder::getId)
                .last("limit 20")));
    }

    private boolean existsPendingLifecycleApproval(Long programId, String actionType) {
        return projectApprovalOrderMapper.selectCount(Wrappers.lambdaQuery(ProjectApprovalOrder.class)
                .eq(ProjectApprovalOrder::getProgramId, programId)
                .eq(ProjectApprovalOrder::getApprovalType, APPROVAL_TYPE_LIFECYCLE)
                .eq(ProjectApprovalOrder::getActionType, actionType)
                .eq(ProjectApprovalOrder::getApprovalStatus, APPROVAL_PENDING)
                .eq(ProjectApprovalOrder::getStatus, BusinessStatus.YES.getCode())) > 0;
    }

    private ProjectApprovalOrder selectPendingApproval(Long approvalId, Long programId) {
        ProjectApprovalOrder approvalOrder = projectApprovalOrderMapper.selectOne(Wrappers.lambdaQuery(ProjectApprovalOrder.class)
                .eq(ProjectApprovalOrder::getId, approvalId)
                .eq(ProjectApprovalOrder::getProgramId, programId)
                .eq(ProjectApprovalOrder::getApprovalStatus, APPROVAL_PENDING)
                .eq(ProjectApprovalOrder::getStatus, BusinessStatus.YES.getCode()));
        if (Objects.isNull(approvalOrder)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return approvalOrder;
    }

    private void validateNotSelfApproval(ProjectApprovalOrder approvalOrder,
                                         BackManageOperatorContextService.OperatorContext operatorContext) {
        boolean sameOperatorId = Objects.nonNull(approvalOrder.getRequesterId())
                && Objects.nonNull(operatorContext.operatorId())
                && Objects.equals(approvalOrder.getRequesterId(), operatorContext.operatorId());
        boolean sameOperatorAccount = isSameNotBlankText(approvalOrder.getRequesterAccount(),
                operatorContext.operatorAccount());
        if (sameOperatorId || sameOperatorAccount) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR.getCode(), "审批发起人不能审批自己");
        }
    }

    private boolean isSameNotBlankText(String left, String right) {
        return StringUtil.isNotEmpty(left) && StringUtil.isNotEmpty(right)
                && left.trim().equalsIgnoreCase(right.trim());
    }

    private List<ProjectApprovalOrderVo> buildApprovalOrderVoList(List<ProjectApprovalOrder> approvalOrderList) {
        return approvalOrderList.stream().map(this::buildApprovalOrderVo).toList();
    }

    private ProjectApprovalOrderVo buildApprovalOrderVo(ProjectApprovalOrder approvalOrder) {
        ProjectApprovalOrderVo vo = new ProjectApprovalOrderVo();
        BeanUtils.copyProperties(approvalOrder, vo);
        vo.setFromLifecycleStatusName(ProjectLifecycleStatus.getMsg(approvalOrder.getFromLifecycleStatus()));
        vo.setTargetLifecycleStatusName(ProjectLifecycleStatus.getMsg(approvalOrder.getTargetLifecycleStatus()));
        vo.setApprovalStatusName(approvalStatusName(approvalOrder.getApprovalStatus()));
        return vo;
    }

    private String approvalStatusName(Integer approvalStatus) {
        if (Objects.equals(approvalStatus, APPROVAL_PENDING)) {
            return "待审批";
        }
        if (Objects.equals(approvalStatus, APPROVAL_APPROVED)) {
            return "已通过";
        }
        if (Objects.equals(approvalStatus, APPROVAL_REJECTED)) {
            return "已驳回";
        }
        return "未知";
    }

    private String buildLifecycleApprovalSnapshot(Program program, ProjectLifecycle lifecycle,
                                                  ProjectLifecycleTransitionDto dto) {
        return JSON.toJSONString(new LifecycleApprovalSnapshot(program.getId(), program.getTitle(),
                lifecycle.getLifecycleStatus(), dto.getActionType(), dto.getRemark(), dto.getConfirmText()));
    }

    private String normalizeText(String value) {
        return Objects.isNull(value) ? null : value.trim();
    }

    private record LifecycleApprovalSnapshot(Long programId, String title, Integer lifecycleStatus,
                                             String actionType, String remark, String confirmText) {
    }
}
