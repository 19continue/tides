package com.tides.service.composite.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.tides.dto.ProgramOrderCreateDto;
import com.tides.dto.SeatDto;
import com.tides.enums.BaseCode;
import com.tides.exception.TidesFrameException;
import com.tides.service.composite.AbstractProgramCheckHandler;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @description: 节目订单参数检查
 * @author: 19continue
 **/
@Component
public class ProgramOrderCreateParamCheckHandler extends AbstractProgramCheckHandler {
    
    @Override
    protected void execute(final ProgramOrderCreateDto programOrderCreateDto) {
        List<SeatDto> seatDtoList = programOrderCreateDto.getSeatDtoList();
        List<Long> ticketUserIdList = programOrderCreateDto.getTicketUserIdList();
        if (CollectionUtil.isEmpty(ticketUserIdList)) {
            throw new TidesFrameException(BaseCode.TICKET_USER_EMPTY);
        }
        Map<Long, List<Long>> ticketUserIdMap = 
                ticketUserIdList.stream().collect(Collectors.groupingBy(ticketUserId -> ticketUserId));
        for (List<Long> value : ticketUserIdMap.values()) {
            if (value.size() > 1) {
                throw new TidesFrameException(BaseCode.TICKET_USER_ID_REPEAT);
            }
        }
        if (CollectionUtil.isNotEmpty(seatDtoList)) {
            if (seatDtoList.size() != programOrderCreateDto.getTicketUserIdList().size()) {
                throw new TidesFrameException(BaseCode.TICKET_USER_COUNT_UNEQUAL_SEAT_COUNT);
            }
            Set<Long> seatIdSet = new HashSet<>();
            for (SeatDto seatDto : seatDtoList) {
                if (Objects.isNull(seatDto.getId())) {
                    throw new TidesFrameException(BaseCode.SEAT_ID_EMPTY);
                }
                if (!seatIdSet.add(seatDto.getId())) {
                    throw new TidesFrameException(BaseCode.SEAT_ID_REPEAT);
                }
                if (Objects.isNull(seatDto.getTicketCategoryId())) {
                    throw new TidesFrameException(BaseCode.SEAT_TICKET_CATEGORY_ID_EMPTY);
                }
                if (Objects.isNull(seatDto.getRowCode())) {
                    throw new TidesFrameException(BaseCode.SEAT_ROW_CODE_EMPTY);
                }
                if (Objects.isNull(seatDto.getColCode())) {
                    throw new TidesFrameException(BaseCode.SEAT_COL_CODE_EMPTY);
                }
                if (Objects.isNull(seatDto.getPrice())) {
                    throw new TidesFrameException(BaseCode.SEAT_PRICE_EMPTY);
                }
            }
        }else {
            if (Objects.isNull(programOrderCreateDto.getTicketCategoryId())) {
                throw new TidesFrameException(BaseCode.TICKET_CATEGORY_NOT_EXIST);
            }
            if (Objects.isNull(programOrderCreateDto.getTicketCount())) {
                throw new TidesFrameException(BaseCode.TICKET_COUNT_NOT_EXIST);
            }
            if (programOrderCreateDto.getTicketCount() <= 0) {
                throw new TidesFrameException(BaseCode.TICKET_COUNT_ERROR);
            }
            if (programOrderCreateDto.getTicketCount() != ticketUserIdList.size()) {
                throw new TidesFrameException(BaseCode.TICKET_USER_COUNT_UNEQUAL_SEAT_COUNT);
            }
        }
    }
    
    @Override
    public Integer executeParentOrder() {
        return 0;
    }
    
    @Override
    public Integer executeTier() {
        return 1;
    }
    
    @Override
    public Integer executeOrder() {
        return 1;
    }
}
