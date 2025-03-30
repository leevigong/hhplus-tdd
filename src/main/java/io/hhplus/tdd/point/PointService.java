package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.Lock;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final LockManager lockManager;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable, LockManager lockManager) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
        this.lockManager = lockManager;
    }

    public UserPoint getPoint(Long id) {
        return userPointTable.selectById(id);
    }

    public UserPoint usePoint(Long id, long amount) {
        Lock lock = lockManager.getLock(id); // id로 부터 lock을 가져온다
        lock.lock();

        try {
            // userPoint 조회
            UserPoint userPoint = userPointTable.selectById(id);

            // userPoint 사용
            UserPoint usedUserPoint = userPoint.use(amount);

            // 사용된 userPoint 저장
            userPointTable.insertOrUpdate(usedUserPoint.id(), usedUserPoint.point());

            // pointHistory 저장
            pointHistoryTable.insert(id, amount, TransactionType.USE, usedUserPoint.updateMillis());

            return usedUserPoint;
        } finally {
            lock.unlock();
        }
    }

    public UserPoint chargePoint(Long id, long amount) {
        Lock lock = lockManager.getLock(id); // id로 부터 lock을 가져온다
        lock.lock();

        try {
            // userPoint 조회
            UserPoint userPoint = userPointTable.selectById(id);

            // userPoint 충전
            UserPoint chargedUserPoint = userPoint.charge(amount);

            // 충전된 userPoint 저장
            userPointTable.insertOrUpdate(chargedUserPoint.id(), chargedUserPoint.point());

            // pointHistory 저장
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return chargedUserPoint;
        } finally {
            lock.unlock();
        }
    }

    public List<PointHistory> getUserPointHistory(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }
}
