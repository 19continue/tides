package com.tides.service.strategy.impl;

import com.tides.core.RepeatExecuteLimitConstants;
import com.tides.dto.ProgramOrderCreateDto;
import com.tides.enums.CompositeCheckType;
import com.tides.enums.ProgramOrderVersion;
import com.tides.initialize.impl.composite.CompositeContainer;
import com.tides.repeatexecutelimit.annotion.RepeatExecuteLimit;
import com.tides.service.ProgramOrderService;
import com.tides.service.strategy.BaseProgramOrder;
import com.tides.service.strategy.ProgramOrderStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @description: 节目订单v3
 * @author: 19continue
 **/
@Slf4j
@Component
public class ProgramOrderV31Strategy implements ProgramOrderStrategy {
    
    @Autowired
    private ProgramOrderService programOrderService;
    
    @Autowired
    private BaseProgramOrder baseProgramOrder;
    
    @Autowired
    private CompositeContainer compositeContainer;
    
    @RepeatExecuteLimit(
            name = RepeatExecuteLimitConstants.CREATE_PROGRAM_ORDER,
            keys = {"#programOrderCreateDto.userId","#programOrderCreateDto.programId","#programOrderCreateDto.screeningId","#programOrderCreateDto.clientRequestId"})
    @Override
    public String createOrder(ProgramOrderCreateDto programOrderCreateDto) {
        compositeContainer.execute(CompositeCheckType.PROGRAM_ORDER_CREATE_CHECK.getValue(),programOrderCreateDto);
        return programOrderService.createNew(programOrderCreateDto,ProgramOrderVersion.V31_VERSION.getValue());
    }
    
    @Override
    public String version() {
        return ProgramOrderVersion.V31_VERSION.getVersion();
    }
}
