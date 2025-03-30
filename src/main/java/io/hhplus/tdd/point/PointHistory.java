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
            throw new IllegalArgumentException(ErrorMessage.MINIMUM_POINT_REQUIRED);
        }

        if (type == null) {
            throw new NullPointerException(ErrorMessage.INVALID_TRANSACTION_TYPE);
        }
    }
}
