package com.tides.service.composite;

import com.tides.dto.ProgramGetDto;
import com.tides.entity.Program;
import com.tides.enums.BaseCode;
import com.tides.enums.BusinessStatus;
import com.tides.enums.CompositeCheckType;
import com.tides.exception.TidesFrameException;
import com.tides.handler.BloomFilterHandler;
import com.tides.initialize.impl.composite.AbstractComposite;
import com.tides.mapper.ProgramMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @description: 节目详情查询验证
 * @author: 19continue
 **/
@Component
public class ProgramBloomFilterCheckHandler extends AbstractComposite<ProgramGetDto> {
    
    @Autowired
    private BloomFilterHandler bloomFilterHandler;

    @Autowired
    private ProgramMapper programMapper;
    
    @Override
    protected void execute(final ProgramGetDto param) {
        boolean contains = bloomFilterHandler.contains(String.valueOf(param.getId()));
        if (!contains) {
            Program program = programMapper.selectOne(Wrappers.lambdaQuery(Program.class)
                    .select(Program::getId)
                    .eq(Program::getId, param.getId())
                    .eq(Program::getStatus, BusinessStatus.YES.getCode())
                    .last("limit 1"));
            if (program == null) {
                throw new TidesFrameException(BaseCode.PROGRAM_NOT_EXIST);
            }
            bloomFilterHandler.add(String.valueOf(param.getId()));
        }
    }
    
    @Override
    public String type() {
        return CompositeCheckType.PROGRAM_DETAIL_CHECK.getValue();
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
