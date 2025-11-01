package com.tides.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baidu.fsg.uid.UidGenerator;
import com.tides.dto.ProjectApprovalPolicyChangeLogManageDto;
import com.tides.dto.ProjectApprovalPolicyManageDto;
import com.tides.dto.ProjectApprovalPolicySaveDto;
import com.tides.dto.ProjectApprovalPolicyStatusUpdateDto;
import com.tides.entity.ProjectApprovalPolicy;
import com.tides.entity.ProjectApprovalPolicyChangeLog;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.ProjectApprovalPolicyChangeLogMapper;
import com.tides.mapper.ProjectApprovalPolicyMapper;
import com.tides.page.PageUtil;
import com.tides.util.DateUtils;
import com.tides.util.StringUtil;
import com.tides.vo.ProjectApprovalPolicyChangeLogVo;
import com.tides.vo.ProjectApprovalPolicyVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectApprovalPolicyService {

    public static final String ROLE_PLATFORM_SUPER_ADMIN = "PLATFORM_SUPER_ADMIN";
    private static final int REQUIRE_LOGIN = 1;
    private static final String CHANGE_CREATE = "CREATE";
    private static final String CHANGE_UPDATE = "UPDATE";
    private static final String CHANGE_ENABLE = "ENABLE";
    private static final String CHANGE_DISABLE = "DISABLE";

    @Autowired
    private ProjectApprovalPolicyMapper projectApprovalPolicyMapper;
    @Autowired
    private ProjectApprovalPolicyChangeLogMapper projectApprovalPolicyChangeLogMapper;
    @Autowired
    private UidGenerator uidGenerator;
    @Autowired
    private BackManageOperatorContextService backManageOperatorContextService;

    public IPage<ProjectApprovalPolicyVo> projectApprovalPolicyPage(ProjectApprovalPolicyManageDto dto) {
        LambdaQueryWrapper<ProjectApprovalPolicy> queryWrapper = Wrappers.lambdaQuery(ProjectApprovalPolicy.class)
                .eq(StringUtil.isNotEmpty(dto.getApprovalType()), ProjectApprovalPolicy::getApprovalType,
                        dto.getApprovalType())
                .eq(StringUtil.isNotEmpty(dto.getActionType()), ProjectApprovalPolicy::getActionType,
                        dto.getActionType())
                .like(StringUtil.isNotEmpty(dto.getActionName()), ProjectApprovalPolicy::getActionName,
                        dto.getActionName())
                .like(StringUtil.isNotEmpty(dto.getRoleCode()), ProjectApprovalPolicy::getAllowedRoleCodes,
                        dto.getRoleCode())
                .eq(Objects.nonNull(dto.getStatus()), ProjectApprovalPolicy::getStatus, dto.getStatus())
                .orderByDesc(ProjectApprovalPolicy::getCreateTime)
                .orderByDesc(ProjectApprovalPolicy::getId);
        IPage<ProjectApprovalPolicy> page = projectApprovalPolicyMapper.selectPage(PageUtil.getPageParams(dto),
                queryWrapper);
        IPage<ProjectApprovalPolicyVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::buildPolicyVo).toList());
        return result;
    }

    public Long projectApprovalPolicySave(ProjectApprovalPolicySaveDto dto) {
        Date now = DateUtils.now();
        ProjectApprovalPolicy policy = new ProjectApprovalPolicy();
        BeanUtils.copyProperties(dto, policy);
        policy.setAllowedRoleCodes(normalizeRoleCodes(dto.getAllowedRoleCodeList()));
        policy.setEditTime(now);
        if (Objects.isNull(dto.getId())) {
            policy.setId(uidGenerator.getUid());
            policy.setCreateTime(now);
            policy.setStatus(BusinessStatus.YES.getCode());
            projectApprovalPolicyMapper.insert(policy);
            recordPolicyChange(CHANGE_CREATE, null, policy, dto.getRemark());
            return policy.getId();
        } else {
            ProjectApprovalPolicy beforePolicy = selectPolicyById(dto.getId());
            if (Objects.isNull(beforePolicy)) {
                throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
            }
            projectApprovalPolicyMapper.update(policy, Wrappers.lambdaUpdate(ProjectApprovalPolicy.class)
                    .eq(ProjectApprovalPolicy::getId, dto.getId()));
            ProjectApprovalPolicy afterPolicy = selectPolicyById(dto.getId());
            recordPolicyChange(CHANGE_UPDATE, beforePolicy, afterPolicy, dto.getRemark());
            return dto.getId();
        }
    }

    public Boolean projectApprovalPolicyStatusUpdate(ProjectApprovalPolicyStatusUpdateDto dto) {
        ProjectApprovalPolicy beforePolicy = selectPolicyById(dto.getId());
        if (Objects.isNull(beforePolicy)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        ProjectApprovalPolicy policy = new ProjectApprovalPolicy();
        policy.setStatus(dto.getStatus());
        policy.setEditTime(DateUtils.now());
        boolean updated = projectApprovalPolicyMapper.update(policy, Wrappers.lambdaUpdate(ProjectApprovalPolicy.class)
                .eq(ProjectApprovalPolicy::getId, dto.getId())) > 0;
        if (updated) {
            ProjectApprovalPolicy afterPolicy = selectPolicyById(dto.getId());
            String changeType = Objects.equals(dto.getStatus(), BusinessStatus.YES.getCode()) ?
                    CHANGE_ENABLE : CHANGE_DISABLE;
            recordPolicyChange(changeType, beforePolicy, afterPolicy, afterPolicy.getRemark());
        }
        return updated;
    }

    public IPage<ProjectApprovalPolicyChangeLogVo> projectApprovalPolicyChangeLogPage(
            ProjectApprovalPolicyChangeLogManageDto dto) {
        LambdaQueryWrapper<ProjectApprovalPolicyChangeLog> queryWrapper =
                Wrappers.lambdaQuery(ProjectApprovalPolicyChangeLog.class)
                        .eq(Objects.nonNull(dto.getPolicyId()), ProjectApprovalPolicyChangeLog::getPolicyId,
                                dto.getPolicyId())
                        .eq(StringUtil.isNotEmpty(dto.getActionType()), ProjectApprovalPolicyChangeLog::getActionType,
                                dto.getActionType())
                        .eq(StringUtil.isNotEmpty(dto.getChangeType()), ProjectApprovalPolicyChangeLog::getChangeType,
                                dto.getChangeType())
                        .like(StringUtil.isNotEmpty(dto.getOperatorName()),
                                ProjectApprovalPolicyChangeLog::getOperatorName, dto.getOperatorName())
                        .like(StringUtil.isNotEmpty(dto.getOperatorAccount()),
                                ProjectApprovalPolicyChangeLog::getOperatorAccount, dto.getOperatorAccount())
                        .eq(ProjectApprovalPolicyChangeLog::getStatus, BusinessStatus.YES.getCode())
                        .orderByDesc(ProjectApprovalPolicyChangeLog::getCreateTime)
                        .orderByDesc(ProjectApprovalPolicyChangeLog::getId);
        IPage<ProjectApprovalPolicyChangeLog> page =
                projectApprovalPolicyChangeLogMapper.selectPage(PageUtil.getPageParams(dto), queryWrapper);
        IPage<ProjectApprovalPolicyChangeLogVo> result = new Page<>(page.getCurrent(), page.getSize(),
                page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::buildChangeLogVo).toList());
        return result;
    }

    public void validateApprovalPermission(String actionType,
                                           BackManageOperatorContextService.OperatorContext operatorContext) {
        ProjectApprovalPolicy policy = selectEnabledPolicy(actionType);
        if (Objects.isNull(policy)) {
            return;
        }
        if (Objects.equals(policy.getRequireLogin(), REQUIRE_LOGIN)
                && (Objects.isNull(operatorContext.operatorId())
                || StringUtil.isEmpty(operatorContext.operatorAccount()))) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR.getCode(), "高危审批需要后台登录态");
        }
        Set<String> allowedRoleSet = splitRoleCodeSet(policy.getAllowedRoleCodes());
        if (allowedRoleSet.isEmpty() || hasAnyRole(operatorContext.roleCodeSet(), allowedRoleSet)) {
            return;
        }
        throw new TidesFrameException(BaseCode.PARAMETER_ERROR.getCode(), "当前账号无该高危动作审批权限");
    }

    private ProjectApprovalPolicy selectEnabledPolicy(String actionType) {
        return projectApprovalPolicyMapper.selectOne(Wrappers.lambdaQuery(ProjectApprovalPolicy.class)
                .eq(ProjectApprovalPolicy::getActionType, actionType)
                .eq(ProjectApprovalPolicy::getStatus, BusinessStatus.YES.getCode())
                .last("limit 1"));
    }

    private ProjectApprovalPolicy selectPolicyById(Long id) {
        return projectApprovalPolicyMapper.selectOne(Wrappers.lambdaQuery(ProjectApprovalPolicy.class)
                .eq(ProjectApprovalPolicy::getId, id)
                .last("limit 1"));
    }

    private Set<String> splitRoleCodeSet(String roleCodes) {
        if (StringUtil.isEmpty(roleCodes)) {
            return Set.of();
        }
        return Arrays.stream(roleCodes.split(","))
                .map(String::trim)
                .filter(StringUtil::isNotEmpty)
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean hasAnyRole(Set<String> operatorRoleSet, Set<String> allowedRoleSet) {
        if (Objects.isNull(operatorRoleSet) || operatorRoleSet.isEmpty()) {
            return false;
        }
        if (operatorRoleSet.contains(ROLE_PLATFORM_SUPER_ADMIN)) {
            return true;
        }
        return operatorRoleSet.stream().anyMatch(allowedRoleSet::contains);
    }

    private ProjectApprovalPolicyVo buildPolicyVo(ProjectApprovalPolicy policy) {
        ProjectApprovalPolicyVo vo = new ProjectApprovalPolicyVo();
        BeanUtils.copyProperties(policy, vo);
        vo.setAllowedRoleCodeList(splitRoleCodeSet(policy.getAllowedRoleCodes()).stream().toList());
        vo.setStatusName(Objects.equals(policy.getStatus(), BusinessStatus.YES.getCode()) ? "启用" : "停用");
        return vo;
    }

    private ProjectApprovalPolicyChangeLogVo buildChangeLogVo(ProjectApprovalPolicyChangeLog changeLog) {
        ProjectApprovalPolicyChangeLogVo vo = new ProjectApprovalPolicyChangeLogVo();
        BeanUtils.copyProperties(changeLog, vo);
        return vo;
    }

    private void recordPolicyChange(String changeType, ProjectApprovalPolicy beforePolicy,
                                    ProjectApprovalPolicy afterPolicy, String remark) {
        ProjectApprovalPolicy sourcePolicy = Objects.nonNull(afterPolicy) ? afterPolicy : beforePolicy;
        if (Objects.isNull(sourcePolicy)) {
            return;
        }
        Date now = DateUtils.now();
        BackManageOperatorContextService.OperatorContext operatorContext =
                backManageOperatorContextService.resolve(null, null);
        ProjectApprovalPolicyChangeLog changeLog = new ProjectApprovalPolicyChangeLog();
        changeLog.setId(uidGenerator.getUid());
        changeLog.setPolicyId(sourcePolicy.getId());
        changeLog.setApprovalType(sourcePolicy.getApprovalType());
        changeLog.setActionType(sourcePolicy.getActionType());
        changeLog.setActionName(sourcePolicy.getActionName());
        changeLog.setChangeType(changeType);
        changeLog.setChangeSummary(buildChangeSummary(changeType, beforePolicy, afterPolicy));
        changeLog.setOperatorId(operatorContext.operatorId());
        changeLog.setOperatorName(operatorContext.operatorName());
        changeLog.setOperatorAccount(operatorContext.operatorAccount());
        changeLog.setOperationSource(operatorContext.operationSource());
        changeLog.setRequestTraceId(operatorContext.requestTraceId());
        changeLog.setClientIp(operatorContext.clientIp());
        changeLog.setUserAgent(operatorContext.userAgent());
        changeLog.setBeforeSnapshotJson(toJson(beforePolicy));
        changeLog.setAfterSnapshotJson(toJson(afterPolicy));
        changeLog.setRemark(remark);
        changeLog.setCreateTime(now);
        changeLog.setEditTime(now);
        changeLog.setStatus(BusinessStatus.YES.getCode());
        projectApprovalPolicyChangeLogMapper.insert(changeLog);
    }

    private String buildChangeSummary(String changeType, ProjectApprovalPolicy beforePolicy,
                                      ProjectApprovalPolicy afterPolicy) {
        ProjectApprovalPolicy sourcePolicy = Objects.nonNull(afterPolicy) ? afterPolicy : beforePolicy;
        String actionName = Objects.isNull(sourcePolicy) ? "-" : sourcePolicy.getActionName();
        if (CHANGE_CREATE.equals(changeType)) {
            return "新增审批策略：" + actionName;
        }
        if (CHANGE_ENABLE.equals(changeType)) {
            return "启用审批策略：" + actionName;
        }
        if (CHANGE_DISABLE.equals(changeType)) {
            return "停用审批策略：" + actionName;
        }
        if (Objects.isNull(beforePolicy) || Objects.isNull(afterPolicy)) {
            return "编辑审批策略：" + actionName;
        }
        List<String> changedFieldList = new ArrayList<>();
        appendChangedField(changedFieldList, "审批类型", beforePolicy.getApprovalType(), afterPolicy.getApprovalType());
        appendChangedField(changedFieldList, "动作编码", beforePolicy.getActionType(), afterPolicy.getActionType());
        appendChangedField(changedFieldList, "动作名称", beforePolicy.getActionName(), afterPolicy.getActionName());
        appendChangedField(changedFieldList, "可审批角色", beforePolicy.getAllowedRoleCodes(),
                afterPolicy.getAllowedRoleCodes());
        appendChangedField(changedFieldList, "防止自审", beforePolicy.getPreventSelfApproval(),
                afterPolicy.getPreventSelfApproval());
        appendChangedField(changedFieldList, "要求登录态", beforePolicy.getRequireLogin(),
                afterPolicy.getRequireLogin());
        appendChangedField(changedFieldList, "风险级别", beforePolicy.getRiskLevel(), afterPolicy.getRiskLevel());
        appendChangedField(changedFieldList, "备注", beforePolicy.getRemark(), afterPolicy.getRemark());
        if (changedFieldList.isEmpty()) {
            return "编辑审批策略：未识别到字段变化";
        }
        return "编辑审批策略，变更：" + String.join("、", changedFieldList);
    }

    private void appendChangedField(List<String> changedFieldList, String fieldName, Object beforeValue,
                                    Object afterValue) {
        if (!Objects.equals(beforeValue, afterValue)) {
            changedFieldList.add(fieldName);
        }
    }

    private String toJson(Object object) {
        return Objects.isNull(object) ? null : JSON.toJSONString(object);
    }

    private String normalizeRoleCodes(List<String> roleCodeList) {
        String roleCodes = roleCodeList.stream()
                .filter(StringUtil::isNotEmpty)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
        if (StringUtil.isEmpty(roleCodes)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        return roleCodes;
    }
}
