package com.tides.service;

import com.tides.client.BaseDataClient;
import com.tides.common.ApiResponse;
import com.tides.core.RedisKeyManage;
import com.tides.dto.GetChannelDataByCodeDto;
import com.tides.enums.BaseCode;
import com.tides.exception.ArgumentError;
import com.tides.exception.ArgumentException;
import com.tides.exception.TidesFrameException;
import com.tides.redis.RedisCache;
import com.tides.redis.RedisKeyBuild;
import com.tides.util.StringUtil;
import com.tides.vo.GetChannelDataVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.tides.constant.GatewayConstant.CODE;

/**
 * @description: 渠道数据获取
 * @author: 19continue
 **/
@Slf4j
@Service
public class ChannelDataService {
    
    private final static String EXCEPTION_MESSAGE = "code参数为空";
    
    @Lazy
    @Autowired
    private BaseDataClient baseDataClient;
    
    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    
    public void checkCode(String code){
        if (StringUtil.isEmpty(code)) {
            ArgumentError argumentError = new ArgumentError();
            argumentError.setArgumentName(CODE);
            argumentError.setMessage(EXCEPTION_MESSAGE);
            List<ArgumentError> argumentErrorList = new ArrayList<>();
            argumentErrorList.add(argumentError);
            throw new ArgumentException(BaseCode.ARGUMENT_EMPTY.getCode(),argumentErrorList);
        }
    }
    
    public GetChannelDataVo getChannelDataByCode(String code){
        checkCode(code);
        GetChannelDataVo channelDataVo = getChannelDataByRedis(code);
        if (Objects.isNull(channelDataVo)) {
            channelDataVo = getChannelDataByClient(code);
            setChannelDataRedis(code,channelDataVo);
        }
        return channelDataVo;
    }
    
    private GetChannelDataVo getChannelDataByRedis(String code){
        return redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.CHANNEL_DATA,code),GetChannelDataVo.class);
    }
    
    private void setChannelDataRedis(String code,GetChannelDataVo getChannelDataVo){
        redisCache.set(RedisKeyBuild.createRedisKey(RedisKeyManage.CHANNEL_DATA,code),getChannelDataVo);
    }
    
    private GetChannelDataVo getChannelDataByClient(String code){
        GetChannelDataByCodeDto getChannelDataByCodeDto = new GetChannelDataByCodeDto();
        getChannelDataByCodeDto.setCode(code);
        
        Future<ApiResponse<GetChannelDataVo>> future = 
                threadPoolExecutor.submit(() -> baseDataClient.getByCode(getChannelDataByCodeDto));
        try {
            ApiResponse<GetChannelDataVo> getChannelDataApiResponse = future.get(10, TimeUnit.SECONDS);
            if (Objects.equals(getChannelDataApiResponse.getCode(), BaseCode.SUCCESS.getCode())) {
                return getChannelDataApiResponse.getData();
            }
        } catch (InterruptedException e) {
            log.error("baseDataClient getByCode Interrupted",e);
            throw new TidesFrameException(BaseCode.THREAD_INTERRUPTED);
        } catch (ExecutionException e) {
            log.error("baseDataClient getByCode execution exception",e);
            throw new TidesFrameException(BaseCode.SYSTEM_ERROR);
        } catch (TimeoutException e) {
            log.error("baseDataClient getByCode timeout exception",e);
            throw new TidesFrameException(BaseCode.EXECUTE_TIME_OUT);
        }
        
        throw new TidesFrameException(BaseCode.CHANNEL_DATA_NOT_EXIST);
    }
}
