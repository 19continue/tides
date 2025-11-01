package com.tides.client;

import com.tides.common.ApiResponse;
import com.tides.dto.TicketUserListDto;
import com.tides.dto.UserGetAndTicketUserListDto;
import com.tides.dto.UserIdDto;
import com.tides.vo.TicketUserVo;
import com.tides.vo.UserGetAndTicketUserListVo;
import com.tides.vo.UserVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

import static com.tides.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

/**
 * @description: 用户服务 feign
 * @author: 19continue
 **/
@Component
@FeignClient(value = SPRING_INJECT_PREFIX_DISTINCTION_NAME+"-"+"user-service",fallback = UserClientFallback.class)
public interface UserClient {
    
    /**
     * 查询用户(通过id)
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping(value = "/user/getById")
    ApiResponse<UserVo> getById(UserIdDto dto);
    

    /**
     * 查询购票人(通过userId)
     * @param dto 参数
     * @return 结果
     * */
    @PostMapping(value = "/ticket/user/list")
    ApiResponse<List<TicketUserVo>> list(TicketUserListDto dto);
    
    /**
     * 查询用户和购票人集合
     * @param dto 参数
     * @return 结果
     */
    @PostMapping(value = "/user/get/user/ticket/list")
    ApiResponse<UserGetAndTicketUserListVo> getUserAndTicketUserList(UserGetAndTicketUserListDto dto);
    
}
