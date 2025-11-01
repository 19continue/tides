package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.AreaGetDto;
import com.tides.dto.AreaSelectDto;
import com.tides.dto.GetChannelDataByCodeDto;
import com.tides.enums.BaseCode;
import com.tides.vo.AreaVo;
import com.tides.vo.GetChannelDataVo;
import com.tides.vo.TokenDataVo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 用户服务 feign 异常
 * @author: 19continue
 **/
@Component
public class BaseDataClientFallback implements BaseDataClient{
    @Override
    public ApiResponse<GetChannelDataVo> getByCode(final GetChannelDataByCodeDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<TokenDataVo> get() {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<List<AreaVo>> selectByIdList(final AreaSelectDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<AreaVo> getById(final AreaGetDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
