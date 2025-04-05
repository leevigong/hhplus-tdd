package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class LockManagerTest {

    @Autowired
    LockManager lockManager;

    @Test
    void 동일한_id에_대해_동일한_Lock객체가_반환된다() {
        Long userId = 1L;
        Lock lock1 = lockManager.getLock(userId);
        Lock lock2 = lockManager.getLock(userId);

        assertThat(lock1).isSameAs(lock2);
    }

    @Test
    void 다른_id에_대해_서로_다른_Lock_객체가_반환된다() {
        Lock lock1 = lockManager.getLock(1L);
        Lock lock2 = lockManager.getLock(2L);

        assertThat(lock1).isNotSameAs(lock2);
    }

    @Test
    void 동일한_id에_대해_생성된_ReentrantLock객체는_동일하다() throws InterruptedException, ExecutionException {
        long id = 1L;
        int threadCount = 5;
        CompletableFuture<Lock>[] futures = new CompletableFuture[threadCount];

        // 여러 스레드에서 동일한 id로 Lock 객체를 요청
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> lockManager.getLock(id));
        }

        // 첫 번째 스레드가 반환한 Lock 객체를 기준으로 비교
        Lock firstLock = futures[0].get();
        for (int i = 1; i < threadCount; i++) {
            assertThat(firstLock).isSameAs(futures[i].get());
        }
    }
}
