package com.tides.service.test;

import com.alibaba.fastjson2.JSON;
import com.tides.core.ConsumerTask;
import com.tides.dto.TestSendDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @description: Test
 * @author: 19continue
 **/
@Slf4j
@Service
public class Test implements ConsumerTask {
    
    
    @Override
    public void execute(String content) {
        TestSendDto testSendDto = JSON.parseObject(content, TestSendDto.class);
        log.info("收到消息 : {} 延时: {} 毫秒" ,content,System.currentTimeMillis() - testSendDto.getTime() - 5000);
    }
    
    @Override
    public String topic() {
        return "test-topic";
    }
}
