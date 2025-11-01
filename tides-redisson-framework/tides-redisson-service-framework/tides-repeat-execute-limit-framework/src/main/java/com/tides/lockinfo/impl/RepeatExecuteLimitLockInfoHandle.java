package com.tides.lockinfo.impl;

import com.tides.lockinfo.AbstractLockInfoHandle;

/**
 * @description: 锁信息实现(防重复幂等)
 * @author: 19continue
 **/
public class RepeatExecuteLimitLockInfoHandle extends AbstractLockInfoHandle {

    public static final String PREFIX_NAME = "REPEAT_EXECUTE_LIMIT";
    
    @Override
    protected String getLockPrefixName() {
        return PREFIX_NAME;
    }
}
