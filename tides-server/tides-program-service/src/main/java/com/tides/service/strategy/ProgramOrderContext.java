package com.tides.service.strategy;

import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @description: 节目订单上下文
 * @author: 19continue
 **/
@Component
public class ProgramOrderContext {
    
    private static final Map<String,ProgramOrderStrategy> MAP = new HashMap<>(8);
    
    @Autowired
    private List<ProgramOrderStrategy> programOrderStrategyList;
    
    @PostConstruct
    public void init() {
        for (ProgramOrderStrategy programOrderStrategy : programOrderStrategyList) {
            MAP.put(programOrderStrategy.version(), programOrderStrategy);
        }
    }
    
    public ProgramOrderStrategy get(String version){
        return Optional.ofNullable(MAP.get(version)).orElseThrow(() -> 
                new TidesFrameException(BaseCode.PROGRAM_ORDER_STRATEGY_NOT_EXIST));
    }
}
