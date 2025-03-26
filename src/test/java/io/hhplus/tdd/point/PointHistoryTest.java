package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class PointHistoryTest {

    @Test
    void 충전_포인트_내역을_생성할_수_있다() {
        // given
        long historyId = 1L;
        long userId = 1L;
        long amount = 1000L;
        TransactionType type = TransactionType.CHARGE;

        // when
        PointHistory pointHistory = new PointHistory(historyId, userId, amount, type, System.currentTimeMillis());

        // then
        assertAll(
                () -> assertThat(pointHistory.id()).isEqualTo(historyId),
                () -> assertThat(pointHistory.userId()).isEqualTo(userId),
                () -> assertThat(pointHistory.amount()).isEqualTo(amount),
                () -> assertThat(pointHistory.type()).isEqualTo(type)
        );
    }

    @Test
    void 사용_포인트_내역이_정상적으로_생성된다() {
        // given
        long historyId = 1L;
        long userId = 1L;
        long amount = 1000L;
        TransactionType type = TransactionType.USE;

        // when
        PointHistory pointHistory = new PointHistory(historyId, userId, amount, type, System.currentTimeMillis());

        // then
        assertAll(
                () -> assertThat(pointHistory.id()).isEqualTo(historyId),
                () -> assertThat(pointHistory.userId()).isEqualTo(userId),
                () -> assertThat(pointHistory.amount()).isEqualTo(amount),
                () -> assertThat(pointHistory.type()).isEqualTo(type)
        );
    }

    @Test
    void 충전_포인트_내역은_1보다_작은_값이_들어갈_수_없다() {
        // given
        long historyId = 1L;
        long userId = 1L;
        long amount = 0L;
        TransactionType type = TransactionType.CHARGE;

        // when & then
        assertThatThrownBy(() -> new PointHistory(historyId, userId, amount, type, System.currentTimeMillis()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 및 사용 포인트는 1 이상이어야 합니다.");
    }

    @Test
    void 사용_포인트_내역은_1보다_작은_값이_들어갈_수_없다() {
        // given
        long historyId = 1L;
        long userId = 1L;
        long amount = 0L;
        TransactionType type = TransactionType.USE;

        // when & then
        assertThatThrownBy(() -> new PointHistory(historyId, userId, amount, type, System.currentTimeMillis()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 및 사용 포인트는 1 이상이어야 합니다.");
    }
    
    @Test
    void 포인트_내역_타입이_null이면_NPE가_발생한다() {
        // given
        long historyId = 1L;
        long userId = 1L;
        long amount = 1000L;
        TransactionType type = null;

        // when & then
        assertThatThrownBy(() -> new PointHistory(historyId, userId, amount, type, System.currentTimeMillis()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("TransactionType이 null 입니다.");
    }
}
