package com.tides.pay;

import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @description: 支付策略上下文
 * @author: 19continue
 **/
public class PayStrategyContext {
    
    private final Map<String,PayStrategyHandler> payStrategyHandlerMap = new HashMap<>();
    
    public void put(String channel,PayStrategyHandler payStrategyHandler){
        payStrategyHandlerMap.put(channel,payStrategyHandler);
    }
    
    public PayStrategyHandler get(String channel){
        return Optional.ofNullable(payStrategyHandlerMap.get(channel)).orElseThrow(
                () -> new TidesFrameException(BaseCode.PAY_STRATEGY_NOT_EXIST));
    }
}
