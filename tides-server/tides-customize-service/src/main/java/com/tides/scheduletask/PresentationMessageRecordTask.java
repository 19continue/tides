package com.tides.scheduletask;

import com.tides.BusinessThreadPool;
import com.tides.service.MessageRecordService;
import com.tides.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @description: 删除消息记录定时任务
 * @author: 19continue
 **/
@Slf4j
@Component
public class PresentationMessageRecordTask {
    
    @Autowired
    private MessageRecordService messageRecordService;
    
    @Scheduled(cron = "0 0 23 * * ?")
    public void executeTask(){
        BusinessThreadPool.execute( () -> {
            //删除所有的消息记录数据
            log.info("开始删除所有消息记录数据");
            messageRecordService.deleteMessageRecord(DateUtils.now());
        });
    }
}
