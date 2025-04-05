package io.hhplus.tdd.point;


import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PointControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PointService pointService;

    @Test
    void 유저_포인트_조회_요청시_UserPoint를_반환한다() throws Exception {
        // given
        long userId = 1L;
        UserPoint userPoint = UserPoint.empty(userId);

        given(pointService.getPoint(userId)).willReturn(userPoint);

        // when & then
        mockMvc.perform(get("/point/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(0L))
                .andExpect(jsonPath("$.updateMillis").exists());
    }

    @Nested
    class 유저포인트_내역_조회 {
        @Test
        void 유저포인트_내역_존재하면_PointHistory리스트를_반환한다() throws Exception {
            // given
            long userId = 1L;
            PointHistory pointHistory1 = new PointHistory(1L, userId, 3000L, TransactionType.CHARGE, System.currentTimeMillis());
            PointHistory pointHistory2 = new PointHistory(2L, userId, 4000L, TransactionType.CHARGE, System.currentTimeMillis());
            PointHistory pointHistory3 = new PointHistory(3L, userId, 5000L, TransactionType.CHARGE, System.currentTimeMillis());
            List<PointHistory> pointHistories = List.of(pointHistory1, pointHistory2, pointHistory3);

            given(pointService.getUserPointHistory(userId)).willReturn(pointHistories);

            // when & then
            mockMvc.perform(get("/point/{id}/histories", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[0].id").value(userId))
                    .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        void 유저포인트_내역이_없으면_빈_리스트를_반환한다() throws Exception {
            // given
            long userId = 1L;
            List<PointHistory> pointHistories = List.of();

            given(pointService.getUserPointHistory(userId)).willReturn(pointHistories);

            // when & then
            mockMvc.perform(get("/point/{id}/histories", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class 포인트_충전 {

        @Test
        void 충전_포인트가_1보다_작으면_IllegalArgumentException을_반환한다() throws Exception {
            // given
            long userId = 1L;

            doThrow(new IllegalArgumentException("충전 및 사용 포인트는 1 이상이어야 합니다."))
                    .when(pointService).chargePoint(anyLong(), eq(0L));

            // when & then
            mockMvc.perform(patch("/point/{id}/charge", userId)
                            .content("0")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(result ->
                            assertThat(result.getResolvedException())
                                    .isInstanceOf(IllegalArgumentException.class)
                                    .hasMessage("충전 및 사용 포인트는 1 이상이어야 합니다.")
                    );
        }

        @Test
        void 충전_포인트와_보유_포인트의_합이_100000_초과하면_IllegalArgumentException을_반환한다() throws Exception {
            // given
            long userId = 1L;
            long currentPoint = 90000L;
            long amount = 10001L;
            UserPoint userPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());

            doThrow(new IllegalArgumentException(ErrorMessage.EXCEED_MAXIMUM_CHARGE_LIMIT))
                    .when(pointService).chargePoint(anyLong(), anyLong());

            // when & then
            mockMvc.perform(patch("/point/{id}/charge", userId)
                            .content(String.valueOf(amount))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(result ->
                            assertThat(result.getResolvedException())
                                    .isInstanceOf(IllegalArgumentException.class)
                                    .hasMessage(ErrorMessage.EXCEED_MAXIMUM_CHARGE_LIMIT)
                    );
        }

        @Test
        void 충전_포인트가_1이상이면_충전이_성공한다() throws Exception {
            // given
            long userId = 1L;
            long currentPoint = 1000L;
            long amount = 1L;
            UserPoint userPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
            UserPoint chargedUserPoint = new UserPoint(userId, currentPoint + amount, System.currentTimeMillis());

            given(pointService.chargePoint(userId, amount)).willReturn(chargedUserPoint);

            // when & then
            mockMvc.perform(patch("/point/{id}/charge", userId)
                            .content("1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.point").value(1001L));
        }
    }

    @Nested
    class 포인트_사용 {

        @Test
        void 사용_포인트가_1보다_작으면_IllegalArgumentException을_반환한다() throws Exception {
            // given
            doThrow(new IllegalArgumentException(ErrorMessage.MINIMUM_POINT_REQUIRED))
                    .when(pointService).usePoint(anyLong(), eq(0L));

            // when & then
            mockMvc.perform(patch("/point/{id}/use", 1L)
                            .content("0")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(result ->
                            assertThat(result.getResolvedException())
                                    .isInstanceOf(IllegalArgumentException.class)
                                    .hasMessage(ErrorMessage.MINIMUM_POINT_REQUIRED)
                    );
        }

        @Test
        void 사용_포인트가_보유_포인트보다_크면_IllegalArgumentException을_반환한다() throws Exception {
            // given
            long userId = 1L;
            long currentPoint = 1000L;
            long usedAmount = 1001L;
            UserPoint userPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());

            doThrow(new IllegalArgumentException(ErrorMessage.INSUFFICIENT_POINTS))
                    .when(pointService).usePoint(anyLong(), anyLong());

            // when & then
            mockMvc.perform(patch("/point/{id}/use", userId)
                            .content(String.valueOf(usedAmount))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError())
                    .andExpect(result ->
                            assertThat(result.getResolvedException())
                                    .isInstanceOf(IllegalArgumentException.class)
                                    .hasMessage(ErrorMessage.INSUFFICIENT_POINTS)
                    );
        }

        @Test
        void 사용_포인트가_1이상이면_충전이_성공한다() throws Exception {
            // given
            long userId = 1L;
            long currentPoint = 1000L;
            long amount = 1L;
            UserPoint userPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
            UserPoint usedUserPoint = new UserPoint(userId, currentPoint - amount, System.currentTimeMillis());

            given(pointService.usePoint(userId, amount)).willReturn(usedUserPoint);

            // when & then
            mockMvc.perform(patch("/point/{id}/use", userId)
                            .content("1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.point").value(999L));
        }
    }
}
