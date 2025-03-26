package io.hhplus.tdd.point.sevice;


import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;

import java.util.List;

public interface PointService {

    // 포인트 조회
    UserPoint getPoint(Long id);

    // 포인트 사용
    UserPoint usePoint(Long id, long amount);

    // 포인트 충전
    UserPoint chargePoint(Long id, long amount);

    // 포인트 내역 조회
    List<PointHistory> getUserPointHistory(Long id);
}
