package com.tides.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description: redis和数据对账结果(节目维度)
 * @author: 19continue
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationProgramResult {
    
    /**
     * 记录标识的集合
     * */
    private List<ExaminationIdentifierResult> examinationIdentifierResultList;
}
