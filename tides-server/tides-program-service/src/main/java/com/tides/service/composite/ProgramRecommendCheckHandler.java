package com.tides.service.composite;

import com.tides.dto.ProgramRecommendListDto;
import com.tides.enums.BaseCode;
import com.tides.enums.CompositeCheckType;
import com.tides.exception.TidesFrameException;
import com.tides.initialize.impl.composite.AbstractComposite;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @description: 节目推荐参数验证
 * @author: 19continue
 **/
@Component
public class ProgramRecommendCheckHandler extends AbstractComposite<ProgramRecommendListDto> {
    
    @Override
    protected void execute(final ProgramRecommendListDto param) {
        if (Objects.isNull(param.getAreaId()) && 
                Objects.isNull(param.getParentProgramCategoryId()) &&
                Objects.isNull(param.getProgramId())) {
            throw new TidesFrameException(BaseCode.PARAMETERS_CANNOT_BE_EMPTY);
        }
    }
    
    @Override
    public String type() {
        return CompositeCheckType.PROGRAM_RECOMMEND_CHECK.getValue();
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
