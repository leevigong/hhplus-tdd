package io.hhplus.tdd.point.sevice;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class PointServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(PointServiceIntegrationTest.class);

    @Autowired
    UserPointTable userPointTable;

    @Autowired
    PointHistoryTable pointHistoryTable;

    @Autowired
    PointService pointService;

    @Test
    void 유저는_자신의_포인트를_조회할_수_있다() {
        // given
        long userId = 1L;
        long currentPoint = 10000L;
        // 초기 userPoint 세팅
        userPointTable.insertOrUpdate(userId, currentPoint);

        // when
        UserPoint userPoint = pointService.getPoint(userId);

        // then
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isEqualTo(currentPoint);
    }

    @Test
    void 유저는_자신의_포인트_내역을_조회할_수_있다() {
        // given
        long userId = 2L;
        pointHistoryTable.insert(userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistoryTable.insert(userId, 2000L, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistoryTable.insert(userId, 3000L, TransactionType.USE, System.currentTimeMillis());

        // when
        List<PointHistory> userPointHistories = pointService.getUserPointHistory(userId);

        // then
        assertAll(
                () -> assertThat(userPointHistories.size()).isEqualTo(3),
                () -> assertThat(userPointHistories.get(0).amount()).isEqualTo(1000L),
                () -> assertThat(userPointHistories.get(1).type()).isEqualTo(TransactionType.CHARGE),
                () -> assertThat(userPointHistories.get(2).type()).isEqualTo(TransactionType.USE)
        );
    }

    @Test
    void 포인트_충전시_포인트가_업데이트되고_충전내역이_저장된다() {
        // given
        long userId = 3L;
        long chargeAmount = 100L;

        // when
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // then
        UserPoint userPoint = pointService.getPoint(userId);
        List<PointHistory> userPointHistory = pointService.getUserPointHistory(userId);
        assertAll(
                () -> assertThat(userPoint.id()).isEqualTo(result.id()),
                () -> assertThat(userPoint.point()).isEqualTo(result.point()),

                () -> assertThat(userPointHistory.size()).isEqualTo(1),
                () -> assertThat(userPointHistory.get(0).userId()).isEqualTo(userId),
                () -> assertThat(userPointHistory.get(0).type()).isEqualTo(TransactionType.CHARGE),
                () -> assertThat(userPointHistory.get(0).amount()).isEqualTo(chargeAmount)
        );
    }

    @Test
    void 포인트_충전시_예외가_발생하면_포인트가_업데이트되지_않는다() {
        // given
        long userId = 4L;
        long currentPoint = 10000L;
        long chargeAmount = 90001L; // 최대 잔고 제한을 위배하는 포인트
        // 초기 userPoint 세팅
        userPointTable.insertOrUpdate(userId, currentPoint);

        // when
        assertThatThrownBy(() -> pointService.chargePoint(userId, chargeAmount));

        // then
        UserPoint userPoint = pointService.getPoint(userId);
        List<PointHistory> userPointHistory = pointService.getUserPointHistory(userId);
        assertAll(
                () -> assertThat(userPoint.id()).isEqualTo(userId),
                () -> assertThat(userPoint.point()).isEqualTo(currentPoint),

                () -> assertThat(userPointHistory.size()).isEqualTo(0)
        );
    }

    @Test
    void 포인트_사용시_포인트가_업데이트되고_포인트_내역이_저장된다() {
        // given
        long userId = 5L;
        long currentPoint = 1000L;
        long useAmount = 100L;

        // 초기 userPoint 세팅
        userPointTable.insertOrUpdate(userId, currentPoint);

        // when
        pointService.usePoint(userId, useAmount);

        // then
        UserPoint userPoint = pointService.getPoint(userId);
        List<PointHistory> userPointHistory = pointService.getUserPointHistory(userId);
        assertAll(
                () -> assertThat(userPoint.id()).isEqualTo(userId),
                () -> assertThat(userPoint.point()).isEqualTo(currentPoint - useAmount),

                () -> assertThat(userPointHistory.size()).isEqualTo(1),
                () -> assertThat(userPointHistory.get(0).userId()).isEqualTo(userId),
                () -> assertThat(userPointHistory.get(0).type()).isEqualTo(TransactionType.USE),
                () -> assertThat(userPointHistory.get(0).amount()).isEqualTo(useAmount)
        );
    }

    @Test
    void 포인트_사용시_에러가_발생하면_포인트가_업데이트되지_않는다() {
        // given
        long userId = 6L;
        long currentPoint = 1000L;
        long useAmount = 1001L; // 현재 보유 포인트보다 큰 포인트

        // 초기 userPoint 세팅
        userPointTable.insertOrUpdate(userId, currentPoint);

        // when
        assertThatThrownBy(() -> pointService.usePoint(userId, useAmount));

        // then
        UserPoint userPoint = pointService.getPoint(userId);
        List<PointHistory> userPointHistory = pointService.getUserPointHistory(userId);
        assertAll(
                () -> assertThat(userPoint.id()).isEqualTo(userId),
                () -> assertThat(userPoint.point()).isEqualTo(currentPoint),

                () -> assertThat(userPointHistory.size()).isEqualTo(0)
        );
    }

    @Test
    void 포인트_충전_및_사용을_하면_포인트가_누적되어_업데이트되고_내역이_저장된다() {
        // given
        long userId = 7L;
        long chargeAmount1 = 100L;
        long chargeAmount2 = 200L;
        long useAmount = 50L;

        // when
        pointService.chargePoint(userId, chargeAmount1);
        pointService.chargePoint(userId, chargeAmount2);
        pointService.usePoint(userId, useAmount);

        // then
        UserPoint userPoint = pointService.getPoint(userId);
        List<PointHistory> userPointHistory = pointService.getUserPointHistory(userId);
        assertAll(
                () -> assertThat(userPoint.id()).isEqualTo(userId),
                () -> assertThat(userPoint.point()).isEqualTo(chargeAmount1 + chargeAmount2 - useAmount),

                () -> assertThat(userPointHistory.size()).isEqualTo(3),
                () -> assertThat(userPointHistory.get(0).userId()).isEqualTo(userId),
                () -> assertThat(userPointHistory.get(0).type()).isEqualTo(TransactionType.CHARGE),
                () -> assertThat(userPointHistory.get(0).amount()).isEqualTo(chargeAmount1),
                () -> assertThat(userPointHistory.get(1).type()).isEqualTo(TransactionType.CHARGE),
                () -> assertThat(userPointHistory.get(1).amount()).isEqualTo(chargeAmount2),
                () -> assertThat(userPointHistory.get(2).type()).isEqualTo(TransactionType.USE),
                () -> assertThat(userPointHistory.get(2).amount()).isEqualTo(useAmount)
        );
    }

    @Nested
    class 동시성_제어 {

        // NOTE: 서비스 로직에서 동시성 제어 lock 부분 주석 처리 후 테스트 해보면 1개만 처리되어 에러가 발생
        @Test
        void 단일_유저가_동시에_5번의_충전을_요청하면_정상적으로_반영된다() throws InterruptedException {
            // given
            long userId = 8L;
            long chargeAmount = 1000L;
            int threadCount = 5;

            // 고정된 쓰레드를 가진 풀을 생성
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            // 쓰레드 작업 완료를 기다려주는 latch 생성
            CountDownLatch latch = new CountDownLatch(threadCount);

            // when
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        log.info("작업 시작 {}", index); // 모두 같은 시간에 로그 찍힘
                        pointService.chargePoint(userId, chargeAmount);
                        log.info("작업 끝 {}", index); // 쓰레드 풀 순서대로 작업 처리
                    } finally {
                        latch.countDown();
                        log.info("latch: {}", latch.getCount());
                    }
                });
            }
            latch.await();

            // then
            UserPoint userPoint = pointService.getPoint(userId);
            List<PointHistory> userPointHistory = pointService.getUserPointHistory(userId);
            assertAll(
                    () -> assertThat(userPoint.id()).isEqualTo(userId),
                    () -> assertThat(userPoint.point()).isEqualTo(chargeAmount * threadCount),

                    () -> assertThat(userPointHistory.size()).isEqualTo(threadCount),
                    () -> assertThat(userPointHistory.get(0).userId()).isEqualTo(userId),
                    () -> assertThat(userPointHistory.get(0).updateMillis()).isLessThan(userPointHistory.get(1).updateMillis()) // 223L 차이
            );
        }

        @Test
        void 단일_유저가_동시에_여러번의_충전_및_사용을_요청하면_정상적으로_반영된다() throws InterruptedException {
            // given
            long userId = 9L;
            long chargeAmount = 1000L;
            long useAmount = 500L;
            int threadCount = 5;

            userPointTable.insertOrUpdate(userId, 0L);

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // when
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        log.info("작업 시작 {}", index);
                        pointService.chargePoint(userId, chargeAmount);
                        log.info("충전 완료 {}", index);
                        pointService.usePoint(userId, useAmount);
                        log.info("사용 완료 {}", index);
                    } finally {
                        latch.countDown();
                        log.info("latch: {}", latch.getCount());
                    }
                });
            }
            latch.await();

            // then
            UserPoint userPoint = pointService.getPoint(userId);
            List<PointHistory> userPointHistory = pointService.getUserPointHistory(userId);
            assertAll(
                    () -> assertThat(userPoint.id()).isEqualTo(userId),
                    () -> assertThat(userPoint.point()).isEqualTo(2500),

                    // 각 스레드마다 충전과 사용 내역이 2건씩 기록되어야 하므로
                    () -> assertThat(userPointHistory.size()).isEqualTo(threadCount * 2)
            );

            // 충전 내역과 사용 내역의 개수가 각각 threadCount만큼 있는지 확인
            long chargeCount = userPointHistory.stream()
                    .filter(pointHistory -> pointHistory.type() == TransactionType.CHARGE)
                    .count();
            long useCount = userPointHistory.stream()
                    .filter(pointHistory -> pointHistory.type() == TransactionType.USE)
                    .count();
            assertAll(
                    () -> assertThat(chargeCount).isEqualTo(threadCount),
                    () -> assertThat(useCount).isEqualTo(threadCount)
            );
        }

        @Test
        void 서로다른_유저들이_동시에_충전_요청시_동시에_처리된다() throws InterruptedException {
            // given
            long[] userIds = {10L, 11L, 12L};
            long chargeAmount = 1000L;
            int threadCount = userIds.length;

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            CyclicBarrier barrier = new CyclicBarrier(threadCount);

            // when
            for (long userId : userIds) {
                executorService.submit(() -> {
                    log.info("User {}: 작업 준비 완료", userId); // 같은 시간에 3명 유저 모두 로그 찍힘
                    try {
                        barrier.await(); // 모든 스레드가 barrier에 도달할 때까지 대기
                        log.info("User {}: 시작 준비 완료", userId); // 같은 시간에 3명 유저 모두 로그 찍힘
                        pointService.chargePoint(userId, chargeAmount);
                        log.info("User {}: latch 완료", userId);  // 쓰레드 풀 순서대로 작업 + latch 실행
                    } catch (Exception e) {
                        log.error("User {}: latch 중 오류 발생: {}", userId, e.getMessage(), e);
                    } finally {
                        latch.countDown();
                        log.info("User {}: 작업 완료. 남은 latch count: {}", userId, latch.getCount());
                    }
                });
            }
            latch.await(); // 모든 작업이 끝날 때까지 대기

            // then
            for (long userId : userIds) {
                UserPoint userPoint = pointService.getPoint(userId);
                List<PointHistory> history = pointService.getUserPointHistory(userId);
                log.info("userId={}, point={}, history={}", userId, userPoint.point(), history);
                assertAll(
                        () -> assertThat(userPoint.point()).isEqualTo(chargeAmount),
                        () -> assertThat(history.size()).isEqualTo(1),
                        () -> assertThat(history.get(0).type()).isEqualTo(TransactionType.CHARGE),
                        () -> assertThat(history.get(0).amount()).isEqualTo(chargeAmount)
                );
            }
            UserPoint userPoint1 = pointService.getPoint(userIds[0]);
            UserPoint userPoint2 = pointService.getPoint(userIds[1]);
            UserPoint userPoint3 = pointService.getPoint(userIds[2]);
            assertThat(userPoint1.updateMillis()).isCloseTo(userPoint2.updateMillis(), within(250L));
            assertThat(userPoint2.updateMillis()).isCloseTo(userPoint3.updateMillis(), within(250L));
        }
    }
}
