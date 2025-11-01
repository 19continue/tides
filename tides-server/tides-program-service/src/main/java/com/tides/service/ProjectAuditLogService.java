package com.tides.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tides.dto.ProjectOperationLogManageDto;
import com.tides.entity.ProjectOperationLog;
import com.tides.entity.ProjectOperationLogDiff;
import com.tides.enums.BusinessStatus;
import com.tides.enums.ProjectLifecycleStatus;
import com.tides.mapper.ProjectOperationLogDiffMapper;
import com.tides.mapper.ProjectOperationLogMapper;
import com.tides.page.PageUtil;
import com.tides.util.DateUtils;
import com.tides.util.StringUtil;
import com.tides.vo.ProjectOperationLogVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * 项目操作审计日志 service，负责记录关键后台写操作和详情页审计查询。
 */
@Service
public class ProjectAuditLogService {

    private static final String MODULE_PROJECT = "PROJECT";
    private static final String MODULE_LIFECYCLE = "LIFECYCLE";
    private static final String MODULE_APPROVAL = "APPROVAL";
    private static final String RISK_NORMAL = "NORMAL";
    private static final String RISK_HIGH = "HIGH";
    private static final Map<String, String> FIELD_LABEL_MAP = buildFieldLabelMap();
    private static final List<String> LIFECYCLE_DIFF_FIELDS = List.of(
            "lifecycleStatus",
            "reviewStatus",
            "reviewRemark",
            "lastActionName",
            "lastOperatorName",
            "lastTransitionTime"
    );
    private static final Set<String> TECHNICAL_FIELDS = Set.of(
            "id",
            "programId",
            "createTime",
            "editTime",
            "status",
            "version",
            "serialVersionUID",
            "ownerId",
            "reviewerId",
            "lastOperatorId"
    );
    private static final Set<String> FLAG_FIELDS = Set.of(
            "preSell",
            "relNameTicketEntrance",
            "permitChooseSeat",
            "electronicInvoice"
    );

    @Autowired
    private UidGenerator uidGenerator;

    @Autowired
    private ProjectOperationLogMapper projectOperationLogMapper;

    @Autowired
    private ProjectOperationLogDiffMapper projectOperationLogDiffMapper;

    @Autowired
    private BackManageOperatorContextService backManageOperatorContextService;

    private static Map<String, String> buildFieldLabelMap() {
        Map<String, String> fieldLabelMap = new LinkedHashMap<>();
        fieldLabelMap.put("title", "项目名称");
        fieldLabelMap.put("parentProgramCategoryId", "一级类目");
        fieldLabelMap.put("programCategoryId", "二级类目");
        fieldLabelMap.put("areaId", "城市/区域");
        fieldLabelMap.put("actor", "演员/艺人");
        fieldLabelMap.put("mainActor", "主创/主演");
        fieldLabelMap.put("place", "场馆/地点");
        fieldLabelMap.put("itemPicture", "主视觉");
        fieldLabelMap.put("detail", "详情介绍");
        fieldLabelMap.put("preSell", "预售");
        fieldLabelMap.put("preSellInstruction", "预售说明");
        fieldLabelMap.put("importantNotice", "重要通知");
        fieldLabelMap.put("perOrderLimitPurchaseCount", "每单限购");
        fieldLabelMap.put("perAccountLimitPurchaseCount", "每账号限购");
        fieldLabelMap.put("permitRefund", "退票口径");
        fieldLabelMap.put("refundTicketRule", "退票规则");
        fieldLabelMap.put("refundExplain", "退款说明");
        fieldLabelMap.put("deliveryInstruction", "配送说明");
        fieldLabelMap.put("entryRule", "入场规则");
        fieldLabelMap.put("realTicketPurchaseRule", "实名规则");
        fieldLabelMap.put("childPurchase", "儿童购票");
        fieldLabelMap.put("invoiceSpecification", "发票说明");
        fieldLabelMap.put("abnormalOrderDescription", "异常订单说明");
        fieldLabelMap.put("kindReminder", "温馨提示");
        fieldLabelMap.put("prohibitedItem", "禁止携带");
        fieldLabelMap.put("depositSpecification", "寄存说明");
        fieldLabelMap.put("performanceDuration", "演出/放映时长");
        fieldLabelMap.put("entryTime", "入场时间");
        fieldLabelMap.put("relNameTicketEntrance", "实名入场");
        fieldLabelMap.put("relNameTicketEntranceExplain", "实名入场说明");
        fieldLabelMap.put("permitChooseSeat", "允许选座");
        fieldLabelMap.put("chooseSeatExplain", "选座说明");
        fieldLabelMap.put("electronicDeliveryTicket", "票品方式");
        fieldLabelMap.put("electronicDeliveryTicketExplain", "票品说明");
        fieldLabelMap.put("electronicInvoice", "电子发票");
        fieldLabelMap.put("electronicInvoiceExplain", "电子发票说明");
        fieldLabelMap.put("previousLifecycleStatus", "原生命周期状态");
        fieldLabelMap.put("lifecycleStatus", "生命周期状态");
        fieldLabelMap.put("reviewStatus", "审核状态");
        fieldLabelMap.put("reviewRemark", "审核/操作备注");
        fieldLabelMap.put("lastActionType", "最近动作编码");
        fieldLabelMap.put("lastActionName", "最近动作");
        fieldLabelMap.put("lastOperatorName", "最近操作人");
        fieldLabelMap.put("lastTransitionTime", "流转时间");
        return Collections.unmodifiableMap(fieldLabelMap);
    }

    public void recordBaseInfoSave(Long programId, Long operatorId, String operatorName, String remark,
                                   Object beforeSnapshot, Object afterSnapshot) {
        record(programId, MODULE_PROJECT, "BASE_INFO_SAVE", "保存基础资料", operatorId, operatorName,
                RISK_NORMAL, remark, beforeSnapshot, afterSnapshot);
    }

    public void recordRuleSave(Long programId, Long operatorId, String operatorName, String remark,
                               Object beforeSnapshot, Object afterSnapshot) {
        record(programId, MODULE_PROJECT, "RULE_SAVE", "保存售后与入场规则", operatorId, operatorName,
                RISK_NORMAL, remark, beforeSnapshot, afterSnapshot);
    }

    public void recordLifecycleTransition(Long programId, Long operatorId, String operatorName, String operationType,
                                          String operationName, String remark, Object afterSnapshot,
                                          boolean dangerFlag) {
        record(programId, MODULE_LIFECYCLE, operationType, operationName, operatorId, operatorName,
                dangerFlag ? RISK_HIGH : RISK_NORMAL, remark, null, afterSnapshot);
    }

    public void recordApprovalSubmit(Long programId, Long operatorId, String operatorName, String operationName,
                                     String remark, Object afterSnapshot) {
        record(programId, MODULE_APPROVAL, "APPROVAL_SUBMIT", operationName, operatorId, operatorName,
                RISK_HIGH, remark, null, afterSnapshot);
    }

    public void recordApprovalHandle(Long programId, Long operatorId, String operatorName, String operationName,
                                     String remark, Object beforeSnapshot, Object afterSnapshot) {
        record(programId, MODULE_APPROVAL, "APPROVAL_HANDLE", operationName, operatorId, operatorName,
                RISK_HIGH, remark, beforeSnapshot, afterSnapshot);
    }

    public List<ProjectOperationLogVo> selectRecentProjectOperationLogList(Long programId) {
        List<ProjectOperationLog> logList = projectOperationLogMapper.selectList(
                Wrappers.lambdaQuery(ProjectOperationLog.class)
                        .eq(ProjectOperationLog::getProgramId, programId)
                        .eq(ProjectOperationLog::getStatus, BusinessStatus.YES.getCode())
                        .orderByDesc(ProjectOperationLog::getCreateTime)
                        .last("limit 20"));
        return buildOperationLogVoList(logList);
    }

    public IPage<ProjectOperationLogVo> projectOperationLogPage(ProjectOperationLogManageDto dto) {
        LambdaQueryWrapper<ProjectOperationLog> queryWrapper = Wrappers.lambdaQuery(ProjectOperationLog.class)
                .eq(Objects.nonNull(dto.getProgramId()), ProjectOperationLog::getProgramId, dto.getProgramId())
                .eq(StringUtil.isNotEmpty(dto.getOperationModule()), ProjectOperationLog::getOperationModule,
                        normalizeText(dto.getOperationModule()))
                .eq(StringUtil.isNotEmpty(dto.getOperationType()), ProjectOperationLog::getOperationType,
                        normalizeText(dto.getOperationType()))
                .eq(StringUtil.isNotEmpty(dto.getRiskLevel()), ProjectOperationLog::getRiskLevel,
                        normalizeText(dto.getRiskLevel()))
                .like(StringUtil.isNotEmpty(dto.getOperatorName()), ProjectOperationLog::getOperatorName,
                        normalizeText(dto.getOperatorName()))
                .like(StringUtil.isNotEmpty(dto.getOperatorAccount()), ProjectOperationLog::getOperatorAccount,
                        normalizeText(dto.getOperatorAccount()))
                .eq(StringUtil.isNotEmpty(dto.getOperationSource()), ProjectOperationLog::getOperationSource,
                        normalizeText(dto.getOperationSource()))
                .eq(ProjectOperationLog::getStatus, BusinessStatus.YES.getCode());
        Date createTimeStart = parseQueryTime(dto.getCreateTimeStart(), false);
        Date createTimeEnd = parseQueryTime(dto.getCreateTimeEnd(), true);
        queryWrapper.ge(Objects.nonNull(createTimeStart), ProjectOperationLog::getCreateTime, createTimeStart);
        queryWrapper.le(Objects.nonNull(createTimeEnd), ProjectOperationLog::getCreateTime, createTimeEnd);
        appendDiffFieldCondition(queryWrapper, dto, createTimeStart, createTimeEnd);
        queryWrapper.orderByDesc(ProjectOperationLog::getCreateTime)
                .orderByDesc(ProjectOperationLog::getId);
        IPage<ProjectOperationLog> logPage = projectOperationLogMapper.selectPage(PageUtil.getPageParams(dto),
                queryWrapper);
        IPage<ProjectOperationLogVo> result = new Page<>(logPage.getCurrent(), logPage.getSize(),
                logPage.getTotal());
        result.setRecords(buildOperationLogVoList(logPage.getRecords()));
        return result;
    }

    private void record(Long programId, String operationModule, String operationType, String operationName,
                        Long operatorId, String operatorName, String riskLevel, String remark,
                        Object beforeSnapshot, Object afterSnapshot) {
        Date now = DateUtils.now();
        ProjectOperationLog log = new ProjectOperationLog();
        log.setId(uidGenerator.getUid());
        log.setProgramId(programId);
        log.setOperationModule(operationModule);
        log.setOperationType(operationType);
        log.setOperationName(operationName);
        BackManageOperatorContextService.OperatorContext operatorContext =
                backManageOperatorContextService.resolve(operatorId, operatorName);
        log.setOperatorId(operatorContext.operatorId());
        log.setOperatorName(operatorContext.operatorName());
        log.setOperatorAccount(operatorContext.operatorAccount());
        log.setOperationSource(operatorContext.operationSource());
        log.setRequestTraceId(operatorContext.requestTraceId());
        log.setClientIp(operatorContext.clientIp());
        log.setUserAgent(operatorContext.userAgent());
        log.setRiskLevel(riskLevel);
        log.setRemark(remark);
        log.setBeforeSnapshotJson(toJson(beforeSnapshot));
        log.setAfterSnapshotJson(toJson(afterSnapshot));
        log.setCreateTime(now);
        log.setEditTime(now);
        log.setStatus(BusinessStatus.YES.getCode());
        projectOperationLogMapper.insert(log);
        insertOperationLogDiffList(log, buildDiffList(log), now);
    }

    private List<ProjectOperationLogVo> buildOperationLogVoList(List<ProjectOperationLog> logList) {
        if (logList.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ProjectOperationLogVo.FieldDiff>> persistedDiffMap = selectPersistedDiffMap(logList);
        return logList.stream()
                .map(log -> buildOperationLogVo(log, persistedDiffMap.get(log.getId())))
                .toList();
    }

    private ProjectOperationLogVo buildOperationLogVo(ProjectOperationLog log,
                                                      List<ProjectOperationLogVo.FieldDiff> persistedDiffList) {
        ProjectOperationLogVo vo = new ProjectOperationLogVo();
        BeanUtils.copyProperties(log, vo);
        List<ProjectOperationLogVo.FieldDiff> diffList = Objects.nonNull(persistedDiffList) &&
                !persistedDiffList.isEmpty() ? persistedDiffList : buildDiffList(log);
        vo.setDiffList(diffList);
        vo.setDiffSummary(buildDiffSummary(log, diffList));
        return vo;
    }

    private void insertOperationLogDiffList(ProjectOperationLog log, List<ProjectOperationLogVo.FieldDiff> diffList,
                                            Date now) {
        if (diffList.isEmpty()) {
            return;
        }
        for (ProjectOperationLogVo.FieldDiff diff : diffList) {
            ProjectOperationLogDiff logDiff = new ProjectOperationLogDiff();
            logDiff.setId(uidGenerator.getUid());
            logDiff.setProgramId(log.getProgramId());
            logDiff.setOperationLogId(log.getId());
            logDiff.setFieldName(diff.getFieldName());
            logDiff.setFieldLabel(diff.getFieldLabel());
            logDiff.setBeforeValue(diff.getBeforeValue());
            logDiff.setAfterValue(diff.getAfterValue());
            logDiff.setCreateTime(now);
            logDiff.setEditTime(now);
            logDiff.setStatus(BusinessStatus.YES.getCode());
            projectOperationLogDiffMapper.insert(logDiff);
        }
    }

    private Map<Long, List<ProjectOperationLogVo.FieldDiff>> selectPersistedDiffMap(List<ProjectOperationLog> logList) {
        Map<Long, List<Long>> logIdMap = logList.stream()
                .collect(Collectors.groupingBy(ProjectOperationLog::getProgramId,
                        Collectors.mapping(ProjectOperationLog::getId, Collectors.toList())));
        List<ProjectOperationLogDiff> logDiffList = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> entry : logIdMap.entrySet()) {
            logDiffList.addAll(projectOperationLogDiffMapper.selectList(Wrappers.lambdaQuery(ProjectOperationLogDiff.class)
                    .eq(ProjectOperationLogDiff::getProgramId, entry.getKey())
                    .in(ProjectOperationLogDiff::getOperationLogId, entry.getValue())
                    .eq(ProjectOperationLogDiff::getStatus, BusinessStatus.YES.getCode())
                    .orderByAsc(ProjectOperationLogDiff::getId)));
        }
        return logDiffList.stream()
                .collect(Collectors.groupingBy(ProjectOperationLogDiff::getOperationLogId,
                        Collectors.mapping(this::buildFieldDiffVo, Collectors.toList())));
    }

    private ProjectOperationLogVo.FieldDiff buildFieldDiffVo(ProjectOperationLogDiff logDiff) {
        ProjectOperationLogVo.FieldDiff diff = new ProjectOperationLogVo.FieldDiff();
        diff.setFieldName(logDiff.getFieldName());
        diff.setFieldLabel(logDiff.getFieldLabel());
        diff.setBeforeValue(logDiff.getBeforeValue());
        diff.setAfterValue(logDiff.getAfterValue());
        return diff;
    }

    private List<ProjectOperationLogVo.FieldDiff> buildDiffList(ProjectOperationLog log) {
        JSONObject beforeSnapshot = parseObject(log.getBeforeSnapshotJson());
        JSONObject afterSnapshot = parseObject(log.getAfterSnapshotJson());
        if (Objects.isNull(afterSnapshot) || afterSnapshot.isEmpty()) {
            return List.of();
        }
        if (MODULE_LIFECYCLE.equals(log.getOperationModule())) {
            return buildLifecycleDiffList(beforeSnapshot, afterSnapshot);
        }
        return buildProjectDiffList(beforeSnapshot, afterSnapshot);
    }

    private List<ProjectOperationLogVo.FieldDiff> buildProjectDiffList(JSONObject beforeSnapshot,
                                                                       JSONObject afterSnapshot) {
        List<ProjectOperationLogVo.FieldDiff> diffList = new ArrayList<>();
        for (String fieldName : resolveProjectDiffFields(afterSnapshot)) {
            Object beforeValue = getValue(beforeSnapshot, fieldName);
            Object afterValue = afterSnapshot.get(fieldName);
            if (!sameValue(fieldName, beforeValue, afterValue)) {
                diffList.add(buildFieldDiff(fieldName, beforeValue, afterValue));
            }
        }
        return diffList;
    }

    private List<ProjectOperationLogVo.FieldDiff> buildLifecycleDiffList(JSONObject beforeSnapshot,
                                                                         JSONObject afterSnapshot) {
        List<ProjectOperationLogVo.FieldDiff> diffList = new ArrayList<>();
        Object previousLifecycleStatus = afterSnapshot.get("previousLifecycleStatus");
        Object lifecycleStatus = afterSnapshot.get("lifecycleStatus");
        if (!sameValue("lifecycleStatus", previousLifecycleStatus, lifecycleStatus)) {
            diffList.add(buildFieldDiff("lifecycleStatus", previousLifecycleStatus, lifecycleStatus));
        }
        for (String fieldName : LIFECYCLE_DIFF_FIELDS) {
            if ("lifecycleStatus".equals(fieldName) || !afterSnapshot.containsKey(fieldName)) {
                continue;
            }
            Object beforeValue = getValue(beforeSnapshot, fieldName);
            Object afterValue = afterSnapshot.get(fieldName);
            if (!sameValue(fieldName, beforeValue, afterValue)) {
                diffList.add(buildFieldDiff(fieldName, beforeValue, afterValue));
            }
        }
        return diffList;
    }

    private Set<String> resolveProjectDiffFields(JSONObject afterSnapshot) {
        LinkedHashSet<String> fieldSet = new LinkedHashSet<>();
        FIELD_LABEL_MAP.keySet().stream()
                .filter(afterSnapshot::containsKey)
                .filter(fieldName -> !TECHNICAL_FIELDS.contains(fieldName))
                .forEach(fieldSet::add);
        afterSnapshot.keySet().stream()
                .filter(fieldName -> !FIELD_LABEL_MAP.containsKey(fieldName))
                .filter(fieldName -> !TECHNICAL_FIELDS.contains(fieldName))
                .forEach(fieldSet::add);
        return fieldSet;
    }

    private ProjectOperationLogVo.FieldDiff buildFieldDiff(String fieldName, Object beforeValue, Object afterValue) {
        ProjectOperationLogVo.FieldDiff diff = new ProjectOperationLogVo.FieldDiff();
        diff.setFieldName(fieldName);
        diff.setFieldLabel(FIELD_LABEL_MAP.getOrDefault(fieldName, fieldName));
        diff.setBeforeValue(formatValue(fieldName, beforeValue));
        diff.setAfterValue(formatValue(fieldName, afterValue));
        return diff;
    }

    private String buildDiffSummary(ProjectOperationLog log, List<ProjectOperationLogVo.FieldDiff> diffList) {
        if (diffList.isEmpty()) {
            return MODULE_LIFECYCLE.equals(log.getOperationModule()) ? "状态流转已记录" : "未识别到字段变化";
        }
        if (MODULE_LIFECYCLE.equals(log.getOperationModule())) {
            ProjectOperationLogVo.FieldDiff statusDiff = findDiff(diffList, "lifecycleStatus");
            ProjectOperationLogVo.FieldDiff actionDiff = findDiff(diffList, "lastActionName");
            if (Objects.nonNull(statusDiff)) {
                String summary = "状态流转：" + blankToDash(statusDiff.getBeforeValue()) +
                        " -> " + blankToDash(statusDiff.getAfterValue());
                if (Objects.nonNull(actionDiff)) {
                    summary = summary + "；动作：" + blankToDash(actionDiff.getAfterValue());
                }
                return summary;
            }
        }
        StringJoiner joiner = new StringJoiner("、");
        diffList.stream()
                .limit(3)
                .map(ProjectOperationLogVo.FieldDiff::getFieldLabel)
                .forEach(joiner::add);
        String tail = diffList.size() > 3 ? "等" : "";
        return "变更 " + diffList.size() + " 项：" + joiner + tail;
    }

    private ProjectOperationLogVo.FieldDiff findDiff(List<ProjectOperationLogVo.FieldDiff> diffList, String fieldName) {
        return diffList.stream()
                .filter(diff -> Objects.equals(diff.getFieldName(), fieldName))
                .findFirst()
                .orElse(null);
    }

    private boolean sameValue(String fieldName, Object beforeValue, Object afterValue) {
        return Objects.equals(formatValue(fieldName, beforeValue), formatValue(fieldName, afterValue));
    }

    private Object getValue(JSONObject snapshot, String fieldName) {
        return Objects.isNull(snapshot) ? null : snapshot.get(fieldName);
    }

    private JSONObject parseObject(String snapshotJson) {
        if (Objects.isNull(snapshotJson) || snapshotJson.isBlank()) {
            return null;
        }
        try {
            return JSON.parseObject(snapshotJson);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void appendDiffFieldCondition(LambdaQueryWrapper<ProjectOperationLog> queryWrapper,
                                          ProjectOperationLogManageDto dto,
                                          Date createTimeStart,
                                          Date createTimeEnd) {
        if (StringUtil.isEmpty(dto.getDiffFieldName())) {
            return;
        }
        String diffFieldName = normalizeText(dto.getDiffFieldName());
        List<Long> operationLogIdList = selectStructuredDiffLogIdList(dto.getProgramId(), diffFieldName,
                createTimeStart, createTimeEnd);
        if (!operationLogIdList.isEmpty()) {
            queryWrapper.in(ProjectOperationLog::getId, operationLogIdList);
            return;
        }
        appendSnapshotDiffFieldFallback(queryWrapper, diffFieldName);
    }

    private List<Long> selectStructuredDiffLogIdList(Long programId, String diffFieldName,
                                                     Date createTimeStart, Date createTimeEnd) {
        if (Objects.isNull(programId)) {
            return List.of();
        }
        return projectOperationLogDiffMapper.selectList(Wrappers.lambdaQuery(ProjectOperationLogDiff.class)
                        .select(ProjectOperationLogDiff::getOperationLogId)
                        .eq(ProjectOperationLogDiff::getProgramId, programId)
                        .eq(ProjectOperationLogDiff::getFieldName, diffFieldName)
                        .ge(Objects.nonNull(createTimeStart), ProjectOperationLogDiff::getCreateTime, createTimeStart)
                        .le(Objects.nonNull(createTimeEnd), ProjectOperationLogDiff::getCreateTime, createTimeEnd)
                        .eq(ProjectOperationLogDiff::getStatus, BusinessStatus.YES.getCode())
                        .orderByDesc(ProjectOperationLogDiff::getCreateTime)
                        .last("limit 5000"))
                .stream()
                .map(ProjectOperationLogDiff::getOperationLogId)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new));
    }

    private void appendSnapshotDiffFieldFallback(LambdaQueryWrapper<ProjectOperationLog> queryWrapper,
                                                 String diffFieldName) {
        String fieldToken = "\"" + diffFieldName + "\"";
        queryWrapper.and(wrapper -> wrapper
                .like(ProjectOperationLog::getBeforeSnapshotJson, fieldToken)
                .or()
                .like(ProjectOperationLog::getAfterSnapshotJson, fieldToken));
    }

    private Date parseQueryTime(String value, boolean endOfDay) {
        if (StringUtil.isEmpty(value)) {
            return null;
        }
        String text = normalizeText(value);
        if (text.length() == DateUtils.FORMAT_DATE.length()) {
            text = text + (endOfDay ? " 23:59:59" : " 00:00:00");
        }
        if (text.length() == DateUtils.FORMAT_MINUTE.length()) {
            text = text + ":00";
        }
        return DateUtils.parseDateTime(text);
    }

    private String normalizeText(String value) {
        return Objects.isNull(value) ? null : value.trim();
    }

    private String formatValue(String fieldName, Object value) {
        if (Objects.isNull(value)) {
            return "";
        }
        if (FLAG_FIELDS.contains(fieldName)) {
            return formatFlag(value);
        }
        if ("permitRefund".equals(fieldName)) {
            return formatPermitRefund(value);
        }
        if ("electronicDeliveryTicket".equals(fieldName)) {
            return formatElectronicDelivery(value);
        }
        if ("lifecycleStatus".equals(fieldName) || "previousLifecycleStatus".equals(fieldName)) {
            return formatLifecycleStatus(value);
        }
        if ("reviewStatus".equals(fieldName)) {
            return formatReviewStatus(value);
        }
        if ("lastTransitionTime".equals(fieldName) && value instanceof Number number) {
            return DateUtils.formatDateTime(new Date(number.longValue()));
        }
        return String.valueOf(value);
    }

    private String formatFlag(Object value) {
        Integer code = toInteger(value);
        if (Objects.equals(code, 1)) {
            return "是";
        }
        if (Objects.equals(code, 0)) {
            return "否";
        }
        return String.valueOf(value);
    }

    private String formatPermitRefund(Object value) {
        Integer code = toInteger(value);
        if (Objects.equals(code, 0)) {
            return "不支持退";
        }
        if (Objects.equals(code, 1)) {
            return "条件退";
        }
        if (Objects.equals(code, 2)) {
            return "全部可退";
        }
        return String.valueOf(value);
    }

    private String formatElectronicDelivery(Object value) {
        Integer code = toInteger(value);
        if (Objects.equals(code, 0)) {
            return "无票品配送";
        }
        if (Objects.equals(code, 1)) {
            return "电子票";
        }
        if (Objects.equals(code, 2)) {
            return "快递票";
        }
        return String.valueOf(value);
    }

    private String formatLifecycleStatus(Object value) {
        Integer code = toInteger(value);
        String statusName = ProjectLifecycleStatus.getMsg(code);
        return Objects.nonNull(statusName) ? statusName : String.valueOf(value);
    }

    private String formatReviewStatus(Object value) {
        Integer code = toInteger(value);
        if (Objects.equals(code, 0)) {
            return "未审核";
        }
        if (Objects.equals(code, 1)) {
            return "待审核";
        }
        if (Objects.equals(code, 2)) {
            return "审核通过";
        }
        if (Objects.equals(code, 3)) {
            return "审核驳回";
        }
        return String.valueOf(value);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String blankToDash(String value) {
        return Objects.isNull(value) || value.isBlank() ? "-" : value;
    }

    private String toJson(Object snapshot) {
        return Objects.isNull(snapshot) ? null : JSON.toJSONString(snapshot);
    }
}
