package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    private static final long MAX_USER_POINT = 100_000; // 최대 잔고 한도

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 음수일 수 없습니다.");
        }
    }

    public void validateAmount(long amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("충전 및 사용 포인트는 1 이상이어야 합니다.");
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
            throw new IllegalArgumentException(
                    String.format("최대 충전 포인트(%d)를 초과하였습니다.", MAX_USER_POINT)
            );
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
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        return new UserPoint(id, totalPoint, System.currentTimeMillis());
    }
}
