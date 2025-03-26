package io.hhplus.tdd.point;

public record PointHistory(
        long id,
        long userId,
        long amount,
        TransactionType type,
        long updateMillis
) {

    public PointHistory {
        if (amount < 1) {
            throw new IllegalArgumentException("충전 및 사용 포인트는 1 이상이어야 합니다.");
        }

        if (type == null) {
            throw new NullPointerException("TransactionType이 null 입니다.");
        }
    }
}
