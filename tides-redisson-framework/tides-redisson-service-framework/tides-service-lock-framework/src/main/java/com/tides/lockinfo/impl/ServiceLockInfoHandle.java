package com.tides.lockinfo.impl;

import com.tides.lockinfo.AbstractLockInfoHandle;

/**
 * @description: 锁信息实现(分布式锁)
 * @author: 19continue
 **/
public class ServiceLockInfoHandle extends AbstractLockInfoHandle {

    private static final String LOCK_PREFIX_NAME = "SERVICE_LOCK";
    
    @Override
    protected String getLockPrefixName() {
        return LOCK_PREFIX_NAME;
    }
}
