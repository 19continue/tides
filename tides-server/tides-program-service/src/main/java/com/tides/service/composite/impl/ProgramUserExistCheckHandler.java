package com.tides.service.composite.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.tides.client.OrderClient;
import com.tides.client.UserClient;
import com.tides.common.ApiResponse;
import com.tides.core.RedisKeyManage;
import com.tides.dto.ProgramGetDto;
import com.tides.dto.ProgramOrderCreateDto;
import com.tides.dto.TicketUserListDto;
import com.tides.entity.MovieScreening;
import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import com.tides.service.MovieService;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.service.ProgramService;
import com.tides.service.composite.AbstractProgramCheckHandler;
import com.tides.service.tool.TokenExpireManager;
import com.tides.vo.ProgramVo;
import com.tides.vo.TicketUserVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @description: 用户检查
 * @author: 19continue
 **/
@Slf4j
@Component
public class ProgramUserExistCheckHandler extends AbstractProgramCheckHandler {
    
    @Autowired
    private UserClient userClient;
    
    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private OrderClient orderClient;
    
    @Autowired
    private ProgramService programService;

    @Autowired
    private MovieService movieService;
    
    @Autowired
    private TokenExpireManager tokenExpireManager;
    
    @Override
    protected void execute(ProgramOrderCreateDto programOrderCreateDto) {
        List<TicketUserVo> ticketUserVoList = redisCache.getValueIsList(RedisKeyBuild.createRedisKey(
                RedisKeyManage.TICKET_USER_LIST, programOrderCreateDto.getUserId()), TicketUserVo.class);
        if (CollectionUtil.isEmpty(ticketUserVoList)) {
            TicketUserListDto ticketUserListDto = new TicketUserListDto();
            ticketUserListDto.setUserId(programOrderCreateDto.getUserId());
            ApiResponse<List<TicketUserVo>> apiResponse = userClient.list(ticketUserListDto);
            if (Objects.equals(apiResponse.getCode(), BaseCode.SUCCESS.getCode())) {
                ticketUserVoList = apiResponse.getData();
            }else {
                log.error("user client rpc getUserAndTicketUserList select response : {}", JSON.toJSONString(apiResponse));
                throw new TidesFrameException(apiResponse);
            }
        }
        if (CollectionUtil.isEmpty(ticketUserVoList)) {
            throw new TidesFrameException(BaseCode.TICKET_USER_EMPTY);
        }
        Map<Long, TicketUserVo> ticketUserVoMap = ticketUserVoList.stream()
                .collect(Collectors.toMap(TicketUserVo::getId, ticketUserVo -> ticketUserVo, (v1, v2) -> v2));
        for (Long ticketUserId : programOrderCreateDto.getTicketUserIdList()) {
            if (Objects.isNull(ticketUserVoMap.get(ticketUserId))) {
                throw new TidesFrameException(BaseCode.TICKET_USER_EMPTY);
            }
        }
        if (Objects.nonNull(programOrderCreateDto.getScreeningId())) {
            MovieScreening movieScreening = movieService.selectSellingScreening(programOrderCreateDto.getScreeningId());
            if (!Objects.equals(movieScreening.getProgramId(), programOrderCreateDto.getProgramId())) {
                throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
            }
        } else {
            ProgramGetDto programGetDto = new ProgramGetDto();
            programGetDto.setId(programOrderCreateDto.getProgramId());
            ProgramVo programVo = programService.detailV2(programGetDto);
            if (Objects.isNull(programVo)) {
                throw new TidesFrameException(BaseCode.PROGRAM_NOT_EXIST);
            }
        }
//        Integer count = 0;
//        if (redisCache.hasKey(RedisKeyBuild.createRedisKey(RedisKeyManage.ACCOUNT_ORDER_COUNT,
//                programOrderCreateDto.getUserId(),programOrderCreateDto.getProgramId()))) {
//            count = redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.ACCOUNT_ORDER_COUNT,
//                    programOrderCreateDto.getUserId(),programOrderCreateDto.getProgramId()), Integer.class);
//        }else {
//            AccountOrderCountDto accountOrderCountDto = new AccountOrderCountDto();
//            accountOrderCountDto.setUserId(programOrderCreateDto.getUserId());
//            accountOrderCountDto.setProgramId(programOrderCreateDto.getProgramId());
//            ApiResponse<AccountOrderCountVo> apiResponse = orderClient.accountOrderCount(accountOrderCountDto);
//            if (Objects.equals(apiResponse.getCode(), BaseCode.SUCCESS.getCode())) {
//                count = Optional.ofNullable(apiResponse.getData()).map(AccountOrderCountVo::getCount).orElse(0);
//                redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.ACCOUNT_ORDER_COUNT,
//                                programOrderCreateDto.getUserId(),
//                                programOrderCreateDto.getProgramId()),
//                        count, tokenExpireManager.getTokenExpireTime() + 1, TimeUnit.MINUTES);
//            }
//        }

        Integer seatCount = Optional.ofNullable(programOrderCreateDto.getSeatDtoList()).map(List::size).orElse(0);

        Integer ticketCount = Optional.ofNullable(programOrderCreateDto.getTicketCount()).orElse(0);
//        if (seatCount != 0) {
//            count = count + seatCount;
//        }else if (ticketCount != 0) {
//            count = count + ticketCount;
//        }
//        if (count > programVo.getPerAccountLimitPurchaseCount()) {
//            throw new TidesFrameException(BaseCode.PER_ACCOUNT_PURCHASE_COUNT_OVER_LIMIT);
//        }
    }
    
    @Override
    public Integer executeParentOrder() {
        return 1;
    }
    
    @Override
    public Integer executeTier() {
        return 2;
    }
    
    @Override
    public Integer executeOrder() {
        return 2;
    }
}
