package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.TicketUserListDto;
import com.tides.dto.UserGetAndTicketUserListDto;
import com.tides.dto.UserIdDto;
import com.tides.enums.BaseCode;
import com.tides.vo.UserGetAndTicketUserListVo;
import com.tides.vo.TicketUserVo;
import com.tides.vo.UserVo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 用户服务 feign 异常
 * @author: 19continue
 **/
@Component
public class UserClientFallback implements UserClient {
    
    @Override
    public ApiResponse<UserVo> getById(final UserIdDto userIdDto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<List<TicketUserVo>> list(final TicketUserListDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
    
    @Override
    public ApiResponse<UserGetAndTicketUserListVo> getUserAndTicketUserList(final UserGetAndTicketUserListDto dto) {
        return ApiResponse.error(BaseCode.SYSTEM_ERROR);
    }
}
