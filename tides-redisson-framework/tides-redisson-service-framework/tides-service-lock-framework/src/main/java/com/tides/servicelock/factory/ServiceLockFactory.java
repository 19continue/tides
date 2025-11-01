package com.tides.servicelock.factory;

import com.tides.core.ManageLocker;
import com.tides.servicelock.LockType;
import com.tides.servicelock.ServiceLocker;
import lombok.AllArgsConstructor;

/**
 * @description: 分布式锁类型工厂
 * @author: 19continue
 **/
@AllArgsConstructor
public class ServiceLockFactory {
    
    private final ManageLocker manageLocker;
    

    public ServiceLocker getLock(LockType lockType){
        ServiceLocker lock;
        switch (lockType) {
            case Fair:
                lock = manageLocker.getFairLocker();
                break;
            case Write:
                lock = manageLocker.getWriteLocker();
                break;
            case Read:
                lock = manageLocker.getReadLocker();
                break;
            default:
                lock = manageLocker.getReentrantLocker();
                break;
        }
        return lock;
    }
}
