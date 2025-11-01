package com.tides.service.delayconsumer;

import com.alibaba.fastjson.JSON;
import com.tides.core.ConsumerTask;
import com.tides.core.SpringUtil;
import com.tides.dto.ProgramOperateDataDto;
import com.tides.service.ProgramService;
import com.tides.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.tides.constant.ProgramOrderConstant.DELAY_OPERATE_PROGRAM_DATA_TOPIC;

/**
 * @description: 节目消息监听
 * @author: 19continue
 **/
@Slf4j
@Component
public class DelayOperateProgramDataConsumer implements ConsumerTask {
    
    @Autowired
    private ProgramService programService;
    
    @Override
    public void execute(String content) {
        log.info("延迟操作节目数据消息进行消费 content : {}", content);
        if (StringUtil.isEmpty(content)) {
            log.error("延迟队列消息不存在");
            return;
        }
        ProgramOperateDataDto programOperateDataDto = JSON.parseObject(content, ProgramOperateDataDto.class);
        programService.operateProgramData(programOperateDataDto);
    }
    
    @Override
    public String topic() {
        return SpringUtil.getPrefixDistinctionName() + "-" + DELAY_OPERATE_PROGRAM_DATA_TOPIC;
    }
}
