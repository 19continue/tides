package com.tides.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description: redis和数据对账结果(精简优化)
 * @author: 19continue
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationSimpleResult {

    /**
     * 节目id
     * */
    private Long programId;

    /**
     * 对比结果
     * */
    private List<ExaminationIdentifierResult> examinationIdentifierResultList;

    
}
