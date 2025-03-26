package io.hhplus.tdd.point.sevice;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    UserPointTable userPointTable;

    @Mock
    PointHistoryTable pointHistoryTable;

    @InjectMocks
    PointServiceImpl pointService;

    @Test
    void 유저_포인트를_조회할_수_있다() {
        // given
        long userId = 1L;
        long point = 1000L;
        UserPoint userPoint = new UserPoint(userId, point, System.currentTimeMillis());

        when(userPointTable.selectById(userId))
                .thenReturn(userPoint);

        // when
        UserPoint result = pointService.getPoint(userId);

        // then
        assertAll(
                () -> assertThat(result.id()).isEqualTo(userId),
                () -> assertThat(result.point()).isEqualTo(point)
        );
        verify(userPointTable, times(1)) // 포인트 내역 조회 과정에서 userPointTable의 selectById 메서드가 한 번 호출되었는지 검증
                .selectById(userId);
    }

    @Test
    void 포인트_내역_조회를_할_수_있다() {
        // given
        long userId = 5L;
        PointHistory pointHistory1 = new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory pointHistory2 = new PointHistory(2L, userId, 500L, TransactionType.USE, System.currentTimeMillis());
        PointHistory pointHistory3 = new PointHistory(3L, userId, 2000L, TransactionType.CHARGE, System.currentTimeMillis());

        List<PointHistory> pointHistoryList = List.of(pointHistory1, pointHistory2, pointHistory3);
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(pointHistoryList);

        // when
        List<PointHistory> result = pointService.getUserPointHistory(userId);

        // then
        assertAll(
                () -> assertThat(result).isEqualTo(pointHistoryList),
                () -> assertThat(result.size()).isEqualTo(3)
        );
        verify(pointHistoryTable, times(1)) // 포인트 내역 조회 과정에서 userPointTable의 selectAllByUserId 메서드가 한 번 호출되었는지 검증
                .selectAllByUserId(userId);

    }

    @Test
    void 포인트를_충전하면_포인트가_늘어나고_충전내역_기록이_남는다() {
        // given
        long userId = 1L;
        long point = 1000L;
        long timestamp = 1L;
        long amount = 1000L;

        UserPoint userPoint = new UserPoint(userId, point, timestamp);
        UserPoint chargeUserPoint = new UserPoint(userId, point + amount, timestamp);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, point + amount)).thenReturn(chargeUserPoint);

        // when
        UserPoint result = pointService.chargePoint(userId, amount);

        // then
        assertAll(
                () -> assertThat(result.id()).isEqualTo(userId),
                () -> assertThat(result.point()).isEqualTo(point + amount),
                () -> assertThat(result.updateMillis()).isPositive()
        );

        verify(userPointTable, times(1))    // 포인트 충전 과정에서 userPointTable의 insertOrUpdate 메서드가 한 번 호출되었는지 검증
                .insertOrUpdate(userId, point + amount);
        verify(pointHistoryTable, times(1)) // 포인트 충전 과정에서 userPointTable의 insert 메서드가 한 번 호출되었는지 검증
                .insert(eq(userId), eq(point + amount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    void 포인트를_사용하면_포인트가_차감되고_사용내역_기록이_남는다() {
        // given
        long userId = 1L;
        long point = 1000L;
        long timestamp = 1L;
        long amount = 100L;

        UserPoint userPoint = new UserPoint(userId, point, timestamp);
        UserPoint useUserPoint = new UserPoint(userId, point - amount, timestamp);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, point - amount)).thenReturn(useUserPoint);

        // when
        UserPoint result = pointService.usePoint(userId, amount);

        // then
        assertAll(
                () -> assertThat(result.id()).isEqualTo(userId),
                () -> assertThat(result.point()).isEqualTo(point - amount),
                () -> assertThat(result.updateMillis()).isPositive()
        );

        verify(userPointTable, times(1))    // 포인트 사용 과정에서 userPointTable의 insertOrUpdate 메서드가 한 번 호출되었는지 검증
                .insertOrUpdate(userId, point - amount);
        verify(pointHistoryTable, times(1)) // 포인트 사용 과정에서 pointHistoryTable의 insert 메서드가 한 번 호출되었는지 검증
                .insert(eq(userId), eq(point - amount), eq(TransactionType.USE), anyLong());
    }
}
