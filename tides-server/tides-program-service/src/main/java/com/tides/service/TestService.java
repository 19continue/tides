package com.tides.service;

import com.baidu.fsg.uid.utils.PaddedAtomicLong;
import com.tides.dto.TestSendDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @description: Test service
 * @author: 19continue
 **/
@Slf4j
@Service
public class TestService {
    
    AtomicLong count = new PaddedAtomicLong(0);
    
    public Boolean reset(final TestSendDto testSendDto) {
        count.set(0);
        return true;
    }
}
