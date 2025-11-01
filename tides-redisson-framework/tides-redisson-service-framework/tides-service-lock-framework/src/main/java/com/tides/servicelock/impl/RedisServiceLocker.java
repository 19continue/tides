package com.tides.servicelock.impl;

import com.tides.servicelock.ServiceLocker;
import lombok.AllArgsConstructor;
import org.redisson.api.RLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Tair Cluster 不支持 Redisson 锁脚本里的动态 redis.call，这里使用基础 Redis 命令实现业务锁。
 */
@AllArgsConstructor
public class RedisServiceLocker implements ServiceLocker {

    private static final long DEFAULT_LEASE_MILLIS = 60_000L;

    private static final long RETRY_INTERVAL_MILLIS = 20L;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    private static final ThreadLocal<Map<String, LockHold>> LOCK_HOLDERS =
            ThreadLocal.withInitial(HashMap::new);

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setResultType(Long.class);
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
    }

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public RLock getLock(String lockKey) {
        return (RLock) Proxy.newProxyInstance(
                RLock.class.getClassLoader(),
                new Class[]{RLock.class},
                new RedisRLockInvocationHandler(lockKey));
    }

    @Override
    public RLock lock(String lockKey) {
        lockInternal(lockKey, DEFAULT_LEASE_MILLIS);
        return getLock(lockKey);
    }

    @Override
    public RLock lock(String lockKey, long leaseTime) {
        lockInternal(lockKey, TimeUnit.SECONDS.toMillis(leaseTime));
        return getLock(lockKey);
    }

    @Override
    public RLock lock(String lockKey, TimeUnit unit, long leaseTime) {
        lockInternal(lockKey, unit.toMillis(leaseTime));
        return getLock(lockKey);
    }

    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, long waitTime) {
        return tryLockInternal(lockKey, unit.toMillis(waitTime), DEFAULT_LEASE_MILLIS);
    }

    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, long waitTime, long leaseTime) {
        return tryLockInternal(lockKey, unit.toMillis(waitTime), unit.toMillis(leaseTime));
    }

    @Override
    public void unlock(String lockKey) {
        unlockInternal(lockKey);
    }

    @Override
    public void unlock(RLock lock) {
        lock.unlock();
    }

    private void lockInternal(String lockKey, long leaseMillis) {
        while (!tryLockInternal(lockKey, RETRY_INTERVAL_MILLIS, leaseMillis)) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean tryLockInternal(String lockKey, long waitMillis, long leaseMillis) {
        Map<String, LockHold> holders = LOCK_HOLDERS.get();
        LockHold lockHold = holders.get(lockKey);
        if (Objects.nonNull(lockHold)) {
            lockHold.count++;
            stringRedisTemplate.expire(lockKey, Duration.ofMillis(normalizeLeaseMillis(leaseMillis)));
            return true;
        }

        long normalizedWaitMillis = Math.max(waitMillis, 0L);
        long deadline = System.currentTimeMillis() + normalizedWaitMillis;
        String token = UUID.randomUUID().toString();
        do {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey, token, Duration.ofMillis(normalizeLeaseMillis(leaseMillis)));
            if (Boolean.TRUE.equals(acquired)) {
                holders.put(lockKey, new LockHold(token));
                return true;
            }
            if (normalizedWaitMillis == 0L) {
                return false;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(RETRY_INTERVAL_MILLIS));
        } while (System.currentTimeMillis() <= deadline);
        return false;
    }

    private void unlockInternal(String lockKey) {
        Map<String, LockHold> holders = LOCK_HOLDERS.get();
        LockHold lockHold = holders.get(lockKey);
        if (Objects.isNull(lockHold)) {
            return;
        }
        lockHold.count--;
        if (lockHold.count > 0) {
            return;
        }
        holders.remove(lockKey);
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockHold.token);
        if (holders.isEmpty()) {
            LOCK_HOLDERS.remove();
        }
    }

    private long normalizeLeaseMillis(long leaseMillis) {
        return leaseMillis <= 0L ? DEFAULT_LEASE_MILLIS : leaseMillis;
    }

    private boolean isLocked(String lockKey) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey));
    }

    private boolean isHeldByCurrentThread(String lockKey) {
        return LOCK_HOLDERS.get().containsKey(lockKey);
    }

    private final class RedisRLockInvocationHandler implements InvocationHandler {

        private final String lockKey;

        private RedisRLockInvocationHandler(String lockKey) {
            this.lockKey = lockKey;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if ("lock".equals(methodName)) {
                if (Objects.nonNull(args) && args.length == 2 && args[0] instanceof Long && args[1] instanceof TimeUnit) {
                    lockInternal(lockKey, ((TimeUnit) args[1]).toMillis((Long) args[0]));
                } else {
                    lockInternal(lockKey, DEFAULT_LEASE_MILLIS);
                }
                return null;
            }
            if ("tryLock".equals(methodName)) {
                if (Objects.isNull(args) || args.length == 0) {
                    return tryLockInternal(lockKey, 0L, DEFAULT_LEASE_MILLIS);
                }
                if (args.length == 2 && args[0] instanceof Long && args[1] instanceof TimeUnit) {
                    return tryLockInternal(lockKey, ((TimeUnit) args[1]).toMillis((Long) args[0]), DEFAULT_LEASE_MILLIS);
                }
                if (args.length == 3 && args[0] instanceof Long && args[1] instanceof Long && args[2] instanceof TimeUnit) {
                    TimeUnit unit = (TimeUnit) args[2];
                    return tryLockInternal(lockKey, unit.toMillis((Long) args[0]), unit.toMillis((Long) args[1]));
                }
            }
            if ("unlock".equals(methodName)) {
                unlockInternal(lockKey);
                return null;
            }
            if ("forceUnlock".equals(methodName) || "delete".equals(methodName)) {
                return Boolean.TRUE.equals(stringRedisTemplate.delete(lockKey));
            }
            if ("isLocked".equals(methodName)) {
                return isLocked(lockKey);
            }
            if ("isHeldByCurrentThread".equals(methodName)) {
                return isHeldByCurrentThread(lockKey);
            }
            if ("remainTimeToLive".equals(methodName)) {
                Long expire = stringRedisTemplate.getExpire(lockKey, TimeUnit.MILLISECONDS);
                return Objects.isNull(expire) ? -2L : expire;
            }
            if ("getName".equals(methodName) || "toString".equals(methodName)) {
                return lockKey;
            }
            if ("hashCode".equals(methodName)) {
                return lockKey.hashCode();
            }
            if ("equals".equals(methodName)) {
                return proxy == args[0];
            }
            return defaultValue(method.getReturnType());
        }
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        return null;
    }

    private static final class LockHold {

        private final String token;

        private int count = 1;

        private LockHold(String token) {
            this.token = token;
        }
    }
}
