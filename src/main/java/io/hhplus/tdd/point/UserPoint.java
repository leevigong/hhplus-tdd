package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    public static final long MAX_USER_POINT = 100_000; // 최대 잔고 한도

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint {
        if (point < 0) {
            throw new IllegalArgumentException(ErrorMessage.NEGATIVE_POINT_NOT_ALLOWED);
        }
    }

    public void validateAmount(long amount) {
        if (amount < 1) {
            throw new IllegalArgumentException(ErrorMessage.MINIMUM_POINT_REQUIRED);
        }
    }

    /**
     * 포인트 충전
     * <ul>
     *     <li> 최대 잔고 제한: 포인트 잔액은 최대 100,000 포인트를 초과할 수 없습니다.</li>
     *     <li> 최소 충전: 포인트는 최소 1 포인트 이상 충전해야 합니다.</li>
     * </ul>
     *
     * @param amount 충전할 포인트
     * @return UserPoint
     */
    public UserPoint charge(long amount) {
        validateAmount(amount);

        long totalPoint = point + amount;
        if (totalPoint > MAX_USER_POINT) {
            throw new IllegalArgumentException(ErrorMessage.EXCEED_MAXIMUM_CHARGE_LIMIT);
        }

        return new UserPoint(id, totalPoint, System.currentTimeMillis());
    }

    /**
     * 포인트 사용
     * <ul>
     *     <li> 잔고 부족: 사용하려는 포인트보다 보유 포인트가 적을 경우, 포인트 사용이 실패합니다.</li>
     *     <li> 최소 사용: 포인트는 최소 1 포인트 이상 사용해야 합니다.</li>
     * </ul>
     *
     * @param amount 사용할 포인트
     * @return UserPoint
     */
    public UserPoint use(long amount) {
        validateAmount(amount);

        long totalPoint = point - amount;
        if (totalPoint < 0) {
            throw new IllegalArgumentException(ErrorMessage.INSUFFICIENT_POINTS);
        }

        return new UserPoint(id, totalPoint, System.currentTimeMillis());
    }
}
