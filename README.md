## 동시성 문제란?

여러 스레드가 같은 자원에 동시에 접근하여 데이터의 일관성이 깨지는 상황

### 동시성 문제가 발생하는 이유
여러 스레드가 동시에 공유 자원에 접근하고, 이를 수정할 때 발생하는 불일치와 예측 불가능한 실행 순서 때문이다.

### 동시성 제어X 테스트
> 상황: 단일 유저가 포인트를 동시에 여러번 충전 요청  
> 예시: 1000P 충전 요청을 5개 스레드에서 동시에 보냄

기대값: 1000L × 5 = 500L   
실제값: 1000L
<img width="1571" alt="image" src="https://github.com/user-attachments/assets/bccaf6ca-8704-4b72-9c9a-1b3a582b4916" />

**해당 테스트가 실패한 이유**  
5개의 스레드가 **거의 동시에 실행되면서** 현재 포인트 값을 읽고 1000을 더한 값을 저장하는 경우를 생각해보면,  
각 스레드가 동일한 초기 값을 읽어 1000을 계산하게 된다.   
그 결과, **마지막에 저장된 값만 최종 결과로 남아** 5번의 충전이 1번 충전한 것처럼 보이게 된다.


**정리**
1.	읽기-수정-쓰기(RMW) 연산의 비원자성  
      여러 스레드가 동시에 동일한 값을 읽고, 수정한 뒤에 다시 쓰는 과정이 *원자적으로 이루어지지 않으면* 중간에 다른 스레드의 변경 사항을 반영하지 못하게 된다.
2.	경쟁 상태  
      여러 스레드가 동시에 작업을 수행할 때 실행 순서가 보장되지 않아, 예상과 달리 *일부 작업이 덮어쓰여지는 문제가 발생한다.*  
      
따라서, synchronized 블록이나 Lock, 혹은 Atomic 클래스와 같은 동시성 제어 기법을 활용하여 작업을 **원자적으로 수행**해야한다.   
이를 통해 여러 스레드가 동시에 업데이트를 시도하더라도 각 작업이 순차적으로 안전하게 처리되도록 할 수 있다.

### 동시성 문제 해결 방법

**1. synchronized**

- **암묵적인 lock 방식으로 synchronized 블럭으로 동기화를 하면 자동적으로 lock이 잠기고 해제된다.**
- 주요 메서드 wait(), notify()
    - wait()가 호출되면 실행 중이던 쓰레드는 해당 객체의 waiting pool에서 notify()를 기다리고, notify()가 호출되면 해당 객체의 waiting pool에 있던 쓰레드 중에서 임의의 쓰레드 하나만 통지를 받는다.
    - wait()에 매개변수로 대기시간을 설정할 수 있으며 이때 지정된 시간동안만 기다린다.
      즉, 지정된 시간이 지난 후에 자동적으로 notify()가 호출되는 것과 같다.
    - synchronized 블럭 내부에서만 사용할 수 있다.
- waiting 상태인 스레드는 interrupt가 불가능하다.
- Lock의 범위는 객체 단위이다. 즉, 객체당 Lock을 하나만 가질 수 있습니다.

  그렇기 때문에 `synchronized`가 메소드 둘 다 붙어있다면 하나의 메소드가 이미 `lock`을 쥐고 있기 때문에 나머지 메소드는 기다리게 된다.

- 발생할 수 있는 문제점
  1.  **Race Condition(경쟁 상태)**  
  notify()는 그저 waiting pool에서 대기중인 쓰레드 중에 하나를 임의로 선택해서 통지할 뿐이고, 여러 쓰레드가 lock를 얻기 위해 경쟁하게 된다.
  2. **Starvation(기아 현상)**  
  운이 나쁘면 특정 쓰레드는 계속 notify를 받지 못하고 오래 기다리는 기아 현상이 발생할 수 있다.
  이를 막으려면 notifyAll()을 사용해서 모든 쓰레드에게 통지를 해야한다.
  ⚠️ 단, 모든 쓰레드에게 통지를 해도 결국 lock을 얻을 수 있는 쓰레드는 하나뿐이다.
```java
synchronized(lock) {
    // 임계영역
}
```

**2. Lock**

```java
public interface Lock {
		
    // 사용 가능한 경우 lock을 얻습니다. lock을 사용할 수 없는 경우 lock을 얻을 때까지 스레드를 블락(Block) 시킨다.
    void lock();

    // lock과 유사. 차단(Block) 상태일 때 java.lang.InterruptedException를 발생시키면서 다시 실행을 할 수 있다.
    void lockInterruptibly() throws InterruptedException;

    // lock의 non-blocking 버전. 다른 쓰레드에 lock이 걸려있으면 lock을 얻으려고 기다리지 않는다.
    boolean tryLock();

    // lock()은 lock을 얻을 때까지 쓰레드를 블락(Block) 시키므로 쓰레드의 응답성이 나빠질 수 있다. 
    // 응답성이 중요한 경우 지정된 시간을 정해서 그 시간안에 lock을 얻지 못하면 다시 작업을 할지, 포기할지를 정할 수 있다.
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    // lock을 해지: 교착상태를 방지하기 위함이다.
    void unlock();
}
```

**ReentrantLock**
- Lock의 구현체
- **명시적인 lock 방식으로 수동으로 lock을 잠그고 해제해야한다.**
- synchronized보다 더 다양한 기능을 가지고 있다.
    - lock polling을 지원한다.
    - 코드가 단일 블록 형태를 넘어서는 경우 사용 가능 하다.
    - 타임 아웃을 지정할 수 있다.
    - Condition을 적용해서 대기 중인 쓰레드를 선별적으로 깨울 수 있다.
    - lock 획득을 위해 waiting pool에 있는 쓰레드에게 interrupt를 걸 수 있다.
ReentrantLock을 통해 쓰레드를 구별해서 통지하는 등 동기화를 세세하게 컨트롤 해야할 수 있어 **synchronized에서 경쟁 상태, 기아 현상을 해결해 줄 수 있다.**

```java
public class ReentrantLock implements Lock, java.io.Serializable {
    public ReentrantLock() {
        sync = new NonfairSync();
    }
 
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }
}
```
생성자의 매개변수를 true를 주면 lock이 풀렸을 때 가장 오래 기다린 쓰레드가 lock을 획득할 수 있게 공정(fair)하게 처리한다. (단, 공정하게 처리하려면 어떤  쓰레드가 가장 오래 기다렸는지 확인하는 과정이 필요하다.)

### 동시성 테스트 하는 방법

- **ExecutorService**: Java에서 멀티스레드 작업을 간편하게 관리할 수 있게 도와주는 쓰레드 풀  
- **CountDownLatch**: N개의 스레드가 모두 작업을 완료할 때까지 기다리기 위한 동기화 도구
  - 각 스레드는 작업을 마친 후 latch.countDown()을 호출하여 카운트를 1 줄임
  - 메인 스레드는 latch.await()을 호출하고, 카운트가 0이 될 때까지 대기
  - 모든 작업이 완료된 후 결과를 검증하기 위해 사용됨
  - 그렇지 않으면 메인 스레드가 먼저 종료되어 테스트가 제대로 되지 않음

```java
// 고정된 10개의 쓰레드를 가진 풀을 생성한다라는 의미
ExecutorService executorService = Executors.newFixedThreadPool(10);

// 10개의 스레드가 모두 작업이 끝날떄까지 기다리겠다는 의미
CountDownLatch latch = new CountDownLatch(10);
```

---
## 실제 테스트
### synchronized

```java
@Override
public synchronized UserPoint chargePoint(Long id, long amount) {

      // userPoint 조회
      UserPoint userPoint = userPointTable.selectById(id);

      // userPoint 충전
      UserPoint chargedUserPoint = userPoint.charge(amount);

      // 충전된 userPoint 저장
      userPointTable.insertOrUpdate(chargedUserPoint.id(), chargedUserPoint.point());

      // pointHistory 저장
      pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

      return chargedUserPoint;

}
```
**단일_유저가_동시에_5번의_충전을_요청하면_정상적으로_반영된다** -> 테스트 성공  
**서로다른_유저들이_동시에_충전_요청시_동시에_처리된다** ->  테스트 실패  
=> 메소드 전체에 synchronized를 붙이면, 해당 객체의 락을 사용하여 한 번에 1개의 쓰레드(유저 1명)만 메소드 내부에 들어올 수 있게 되므로  

### ReentrantLock + ConcurrentHashMap
**단일_유저가_동시에_5번의_충전을_요청하면_정상적으로_반영된다** ->  테스트 성공  
**서로다른_유저들이_동시에_충전_요청시_동시에_처리된다** ->  테스트 성공  
=> 사용자 ID별로 Lock을 관리하여 같은 사용자의 요청만 직렬, 다른 사용자의 요청은 병렬(동시에)로 처리
