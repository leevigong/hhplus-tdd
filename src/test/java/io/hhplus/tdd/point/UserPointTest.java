package io.hhplus.tdd.point;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserPointTest {

    @Test
    void 유저_생성시_포인트가_양수이면_정상적으로_생성된다() {
        // given
        long userId = 1L;
        long point = 1000L;

        // when & then
        assertDoesNotThrow(
                () -> new UserPoint(userId, point, System.currentTimeMillis())
        );
    }

    @Test
    void 유저_생성시_포인트가_음수이면_예외가_발생한다() {
        // given
        long userId = 1L;
        long point = -1000L;

        // when & then
        assertThatThrownBy(() -> new UserPoint(userId, point, System.currentTimeMillis()))
                .hasMessage(ErrorMessage.NEGATIVE_POINT_NOT_ALLOWED);
    }

    @Nested
    class 포인트_충전 {

        @Test
        void 포인트를_충전할_수_있다() {
            // given
            UserPoint userPoint = UserPoint.empty(1L);
            long amount = 1000;

            // when & then
            assertDoesNotThrow(
                    () -> userPoint.charge(amount)
            );
        }

        @Test
        void 충전_포인트가_1보다_작으면_충전할_수_없다() {
            // given
            UserPoint userPoint = UserPoint.empty(1L);
            long amount = 0;

            // when & then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userPoint.charge(amount)
            );

            assertThat(exception.getMessage()).isEqualTo(ErrorMessage.MINIMUM_POINT_REQUIRED);
        }

        @Test
        void 충전_포인트가_최대_잔고를_초과하면_충전할_수_없다() {
            // given
            UserPoint userPoint = UserPoint.empty(1L);
            long amount = 100001L;

            // when & then
            assertThatThrownBy(() -> userPoint.charge(amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ErrorMessage.EXCEED_MAXIMUM_CHARGE_LIMIT);
        }

        @Test
        void 충전_후_최대_잔고를_초과하면_충전할_수_없다() {
            // given
            UserPoint userPoint = new UserPoint(1L, 50000, 0);
            long amount = 50001;

            // when & then
            assertThatThrownBy(() -> userPoint.charge(amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ErrorMessage.EXCEED_MAXIMUM_CHARGE_LIMIT);
        }
    }

    @Nested
    class 포인트_사용 {

        @Test
        void 포인트_사용할_수_있다() {
            // given
            UserPoint userPoint = new UserPoint(1L, 50000, 0);
            long amount = 1;

            // when & then
            assertDoesNotThrow(
                    () -> userPoint.use(amount)
            );
        }

        @Test
        void 사용_포인트가_1보다_작으면_사용할_수_없다() {
            // given
            UserPoint userPoint = new UserPoint(1L, 50000, 0);
            long amount = 0;

            // when & then
            assertThatThrownBy(() -> userPoint.use(amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ErrorMessage.MINIMUM_POINT_REQUIRED);
        }

        @Test
        void 사용_포인트가_보유_포인트보다_많으면_사용할_수_없다() {
            // given
            UserPoint userPoint = new UserPoint(1L, 50000, 0);
            long amount = 50001;

            // when & then
            assertThatThrownBy(() -> userPoint.use(amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ErrorMessage.INSUFFICIENT_POINTS);
        }
    }
}
