package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.*;
import com.tides.enums.BaseCode;
import com.tides.vo.ProgramRecordTaskVo;
import com.tides.vo.TicketCategoryDetailVo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 节目服务 feign 异常
 * @author: 19continue
 **/
@Component
public class ProgramClientFallback implements ProgramClient {
    
    @Override
    public ApiResponse<Boolean> operateSeatLockAndTicketCategoryRemainNumber(final ReduceRemainNumberDto reduceRemainNumberDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<List<TicketCategoryDetailVo>> selectList(final TicketCategoryListDto ticketCategoryDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<List<Long>> allList() {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<List<ProgramRecordTaskVo>> select(final ProgramRecordTaskListDto programRecordTaskListDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<Integer> update(final ProgramRecordTaskUpdateDto programRecordTaskUpdateDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<Integer> add(final ProgramRecordTaskAddDto orderTicketUserRecordAddDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<Boolean> operateProgramData(final ProgramOperateDataDto programOperateDataDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }

    @Override
    public ApiResponse<List<TicketCategoryDetailVo>> selectListByProgram(TicketCategoryListByProgramDto ticketCategoryListByProgramDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
