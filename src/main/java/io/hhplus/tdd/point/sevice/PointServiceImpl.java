package io.hhplus.tdd.point.sevice;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Override
    public UserPoint getPoint(Long id) {
        return userPointTable.selectById(id);
    }

    @Override
    public UserPoint usePoint(Long id, long amount) {
        // userPoint 조회
        UserPoint userPoint = userPointTable.selectById(id);

        // userPoint 사용
        UserPoint usedUserPoint = userPoint.use(amount);

        // 사용된 userPoint 저장
        userPointTable.insertOrUpdate(usedUserPoint.id(), usedUserPoint.point());

        // pointHistory 저장
        pointHistoryTable.insert(usedUserPoint.id(), usedUserPoint.point(), TransactionType.USE, usedUserPoint.updateMillis());

        return usedUserPoint;
    }

    @Override
    public UserPoint chargePoint(Long id, long amount) {
        // userPoint 조회
        UserPoint userPoint = userPointTable.selectById(id);

        // userPoint 충전
        UserPoint chargedUserPoint = userPoint.charge(amount);

        // 충전된 userPoint 저장
        userPointTable.insertOrUpdate(chargedUserPoint.id(), chargedUserPoint.point());

        // pointHistory 저장
        pointHistoryTable.insert(chargedUserPoint.id(), chargedUserPoint.point(), TransactionType.CHARGE, System.currentTimeMillis());

        return chargedUserPoint;
    }

    @Override
    public List<PointHistory> getUserPointHistory(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }
}
