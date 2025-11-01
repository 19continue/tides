package com.tides.service.composite.impl;


import com.tides.dto.ProgramGetDto;
import com.tides.dto.ProgramOrderCreateDto;
import com.tides.entity.MovieScreening;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.exception.TidesFrameException;
import com.tides.service.MovieService;
import com.tides.service.ProgramService;
import com.tides.service.composite.AbstractProgramCheckHandler;
import com.tides.util.DateUtils;
import com.tides.vo.ProgramVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @description: 节目检查
 * @author: 19continue
 **/
@Component
public class ProgramDetailCheckHandler extends AbstractProgramCheckHandler {
    
    @Autowired
    private ProgramService programService;

    @Autowired
    private MovieService movieService;
    
    @Override
    protected void execute(final ProgramOrderCreateDto programOrderCreateDto) {
        ProgramVo programVo;
        if (Objects.nonNull(programOrderCreateDto.getScreeningId())) {
            MovieScreening movieScreening = movieService.selectSellingScreening(programOrderCreateDto.getScreeningId());
            if (!Objects.equals(movieScreening.getProgramId(), programOrderCreateDto.getProgramId())) {
                throw new TidesFrameException(BaseCode.PARAMETER_ERROR);
            }
            programVo = programService.simpleGetByIdMultipleCache(programOrderCreateDto.getProgramId());
            if (Objects.isNull(programVo)) {
                Long expireTime = Math.max(1L, DateUtils.countBetweenSecond(DateUtils.now(), movieScreening.getShowTime()));
                programVo = programService.getById(programOrderCreateDto.getProgramId(),
                        expireTime, TimeUnit.SECONDS);
            }
        } else {
            ProgramGetDto programGetDto = new ProgramGetDto();
            programGetDto.setId(programOrderCreateDto.getProgramId());
            programVo = programService.detailV2(programGetDto);
        }
        if (programVo.getPermitChooseSeat().equals(BusinessStatus.NO.getCode())) {
            if (Objects.nonNull(programOrderCreateDto.getSeatDtoList())) {
                throw new TidesFrameException(BaseCode.PROGRAM_NOT_ALLOW_CHOOSE_SEAT);
            }
        }
        Integer seatCount = Optional.ofNullable(programOrderCreateDto.getSeatDtoList()).map(List::size).orElse(0);
        Integer ticketCount = Optional.ofNullable(programOrderCreateDto.getTicketCount()).orElse(0);
        Integer perOrderLimitPurchaseCount = Optional.ofNullable(programVo.getPerOrderLimitPurchaseCount())
                .orElse(Integer.MAX_VALUE);
        if (seatCount > perOrderLimitPurchaseCount || ticketCount > perOrderLimitPurchaseCount) {
            throw new TidesFrameException(BaseCode.PER_ORDER_PURCHASE_COUNT_OVER_LIMIT);
        }
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
        return 1;
    }
}
