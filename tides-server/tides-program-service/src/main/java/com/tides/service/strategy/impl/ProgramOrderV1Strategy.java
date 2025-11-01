package com.tides.service.strategy.impl;

import com.tides.core.RepeatExecuteLimitConstants;
import com.tides.dto.ProgramOrderCreateDto;
import com.tides.enums.CompositeCheckType;
import com.tides.enums.ProgramOrderVersion;
import com.tides.initialize.impl.composite.CompositeContainer;
import com.tides.repeatexecutelimit.annotion.RepeatExecuteLimit;
import com.tides.service.ProgramOrderService;
import com.tides.service.strategy.ProgramOrderStrategy;
import com.tides.servicelock.annotion.ServiceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.tides.core.DistributedLockConstants.PROGRAM_ORDER_CREATE_V1;

/**
 * @description: 节目订单v1
 * @author: 19continue
 **/
@Component
public class ProgramOrderV1Strategy implements ProgramOrderStrategy {
    
    @Autowired
    private ProgramOrderService programOrderService;
    
    @Autowired
    private CompositeContainer compositeContainer;
    
    
    @RepeatExecuteLimit(
            name = RepeatExecuteLimitConstants.CREATE_PROGRAM_ORDER,
            keys = {"#programOrderCreateDto.userId","#programOrderCreateDto.programId","#programOrderCreateDto.screeningId"})
    @ServiceLock(name = PROGRAM_ORDER_CREATE_V1,keys = {"#programOrderCreateDto.programId","#programOrderCreateDto.screeningId"})
    @Override
    public String createOrder(final ProgramOrderCreateDto programOrderCreateDto) {
        compositeContainer.execute(CompositeCheckType.PROGRAM_ORDER_CREATE_CHECK.getValue(),programOrderCreateDto);
        return programOrderService.create(programOrderCreateDto,ProgramOrderVersion.V1_VERSION.getValue());
    }
    
    @Override
    public String version() {
        return ProgramOrderVersion.V1_VERSION.getVersion();
    }
}
