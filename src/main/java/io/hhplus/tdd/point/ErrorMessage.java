package io.hhplus.tdd.point;

import static io.hhplus.tdd.point.UserPoint.MAX_USER_POINT;

public class ErrorMessage {

    public static String NEGATIVE_POINT_NOT_ALLOWED = "포인트는 음수일 수 없습니다.";
    public static String MINIMUM_POINT_REQUIRED = "충전 및 사용 포인트는 1 이상이어야 합니다.";
    public static String INSUFFICIENT_POINTS = "포인트가 부족합니다.";
    public static String EXCEED_MAXIMUM_CHARGE_LIMIT = String.format("최대 충전 포인트(%d)를 초과하였습니다.", MAX_USER_POINT);
    public static String INVALID_TRANSACTION_TYPE = "TransactionType이 null 입니다.";
}
