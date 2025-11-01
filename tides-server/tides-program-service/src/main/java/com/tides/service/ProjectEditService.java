package com.tides.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tides.dto.ProjectBaseInfoSaveDto;
import com.tides.dto.ProjectDetailDto;
import com.tides.dto.ProjectRuleSaveDto;
import com.tides.entity.Program;
import com.tides.entity.ProgramCategory;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.exception.TidesFrameException;
import com.tides.mapper.ProgramCategoryMapper;
import com.tides.mapper.ProgramMapper;
import com.tides.util.DateUtils;
import com.tides.vo.ProjectDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

/**
 * 项目详情编辑 service，承载项目建档资料和售后履约规则的分区保存能力。
 */
@Service
public class ProjectEditService {

    private static final Set<Integer> FLAG_VALUES = Set.of(0, 1);
    private static final Set<Integer> PERMIT_REFUND_VALUES = Set.of(0, 1, 2);
    private static final Set<Integer> ELECTRONIC_DELIVERY_VALUES = Set.of(0, 1, 2);

    @Autowired
    private ProgramMapper programMapper;

    @Autowired
    private ProgramCategoryMapper programCategoryMapper;

    @Autowired
    private ProjectManageService projectManageService;

    @Autowired
    private ProjectAuditLogService projectAuditLogService;

    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailVo projectBaseInfoSave(ProjectBaseInfoSaveDto dto) {
        Program beforeProgram = selectProjectProgram(dto.getProjectId());
        validateProgramCategory(dto.getParentProgramCategoryId(), dto.getProgramCategoryId());
        validateFlag(dto.getPreSell());

        Program update = new Program();
        update.setTitle(dto.getTitle());
        update.setParentProgramCategoryId(dto.getParentProgramCategoryId());
        update.setProgramCategoryId(dto.getProgramCategoryId());
        update.setAreaId(dto.getAreaId());
        update.setActor(dto.getActor());
        update.setMainActor(dto.getMainActor());
        update.setPlace(dto.getPlace());
        update.setItemPicture(dto.getItemPicture());
        update.setDetail(dto.getDetail());
        update.setPreSell(dto.getPreSell());
        update.setPreSellInstruction(dto.getPreSellInstruction());
        update.setImportantNotice(dto.getImportantNotice());
        update.setEditTime(DateUtils.now());
        int updateCount = programMapper.update(update, Wrappers.lambdaUpdate(Program.class)
                .eq(Program::getId, dto.getProjectId())
                .eq(Program::getStatus, BusinessStatus.YES.getCode()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        projectAuditLogService.recordBaseInfoSave(dto.getProjectId(), dto.getOperatorId(), dto.getOperatorName(),
                dto.getRemark(), beforeProgram, update);
        return reloadProjectDetail(dto.getProjectId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectDetailVo projectRuleSave(ProjectRuleSaveDto dto) {
        Program beforeProgram = selectProjectProgram(dto.getProjectId());
        validateLimit(dto.getPerOrderLimitPurchaseCount());
        validateLimit(dto.getPerAccountLimitPurchaseCount());
        validateEnum(dto.getPermitRefund(), PERMIT_REFUND_VALUES);
        validateFlag(dto.getRelNameTicketEntrance());
        validateFlag(dto.getPermitChooseSeat());
        validateEnum(dto.getElectronicDeliveryTicket(), ELECTRONIC_DELIVERY_VALUES);
        validateFlag(dto.getElectronicInvoice());

        Program update = new Program();
        update.setPerOrderLimitPurchaseCount(dto.getPerOrderLimitPurchaseCount());
        update.setPerAccountLimitPurchaseCount(dto.getPerAccountLimitPurchaseCount());
        update.setPermitRefund(dto.getPermitRefund());
        update.setRefundTicketRule(dto.getRefundTicketRule());
        update.setRefundExplain(dto.getRefundExplain());
        update.setDeliveryInstruction(dto.getDeliveryInstruction());
        update.setEntryRule(dto.getEntryRule());
        update.setRealTicketPurchaseRule(dto.getRealTicketPurchaseRule());
        update.setChildPurchase(dto.getChildPurchase());
        update.setInvoiceSpecification(dto.getInvoiceSpecification());
        update.setAbnormalOrderDescription(dto.getAbnormalOrderDescription());
        update.setKindReminder(dto.getKindReminder());
        update.setProhibitedItem(dto.getProhibitedItem());
        update.setDepositSpecification(dto.getDepositSpecification());
        update.setPerformanceDuration(dto.getPerformanceDuration());
        update.setEntryTime(dto.getEntryTime());
        update.setRelNameTicketEntrance(dto.getRelNameTicketEntrance());
        update.setRelNameTicketEntranceExplain(dto.getRelNameTicketEntranceExplain());
        update.setPermitChooseSeat(dto.getPermitChooseSeat());
        update.setChooseSeatExplain(dto.getChooseSeatExplain());
        update.setElectronicDeliveryTicket(dto.getElectronicDeliveryTicket());
        update.setElectronicDeliveryTicketExplain(dto.getElectronicDeliveryTicketExplain());
        update.setElectronicInvoice(dto.getElectronicInvoice());
        update.setElectronicInvoiceExplain(dto.getElectronicInvoiceExplain());
        update.setEditTime(DateUtils.now());
        int updateCount = programMapper.update(update, Wrappers.lambdaUpdate(Program.class)
                .eq(Program::getId, dto.getProjectId())
                .eq(Program::getStatus, BusinessStatus.YES.getCode()));
        if (updateCount == 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
        projectAuditLogService.recordRuleSave(dto.getProjectId(), dto.getOperatorId(), dto.getOperatorName(),
                dto.getRemark(), beforeProgram, update);
        return reloadProjectDetail(dto.getProjectId());
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

    private void validateProgramCategory(Long parentProgramCategoryId, Long programCategoryId) {
        ProgramCategory parentCategory = null;
        if (Objects.nonNull(parentProgramCategoryId)) {
            parentCategory = programCategoryMapper.selectOne(Wrappers.lambdaQuery(ProgramCategory.class)
                    .eq(ProgramCategory::getId, parentProgramCategoryId)
                    .eq(ProgramCategory::getType, 1)
                    .eq(ProgramCategory::getStatus, BusinessStatus.YES.getCode()));
            if (Objects.isNull(parentCategory)) {
                throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
            }
        }
        if (Objects.nonNull(programCategoryId)) {
            ProgramCategory programCategory = programCategoryMapper.selectOne(Wrappers.lambdaQuery(ProgramCategory.class)
                    .eq(ProgramCategory::getId, programCategoryId)
                    .eq(ProgramCategory::getType, 2)
                    .eq(ProgramCategory::getStatus, BusinessStatus.YES.getCode()));
            if (Objects.isNull(programCategory) || (Objects.nonNull(parentCategory) &&
                    !Objects.equals(programCategory.getParentId(), parentCategory.getId()))) {
                throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
            }
        }
    }

    private void validateLimit(Integer value) {
        if (Objects.nonNull(value) && value < 0) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
    }

    private void validateFlag(Integer value) {
        validateEnum(value, FLAG_VALUES);
    }

    private void validateEnum(Integer value, Set<Integer> legalValues) {
        if (Objects.nonNull(value) && !legalValues.contains(value)) {
            throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
        }
    }

    private ProjectDetailVo reloadProjectDetail(Long projectId) {
        ProjectDetailDto detailDto = new ProjectDetailDto();
        detailDto.setProjectId(projectId);
        return projectManageService.projectDetail(detailDto);
    }
}
