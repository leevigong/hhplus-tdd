package io.hhplus.tdd.point;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 특정 id에 대한 Lock을 개별 관리하며 Thread-Safe한 ConcurrentHashMap을 사용
 */
@Component
public class LockManager {
    private final ConcurrentHashMap<Long, Lock> lockMap = new ConcurrentHashMap<>();

    /**
     * 주어진 id에 대응하는 ReentrantLock을 반환하며,
     * 해당 id에 대한 락이 없다면 새로운 ReentrantLock을 생성해 lockMap 추가 후 반환합니다.
     *
     * @param id 유저id
     * @return Lock
     */
    public Lock getLock(Long id) {
        return lockMap.computeIfAbsent(id, key -> new ReentrantLock());
    }
}
