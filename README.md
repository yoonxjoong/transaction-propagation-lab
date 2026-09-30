# transaction-propagation-lab

Spring `@Transactional`의 전파(Propagation) 속성이 실제로 어떻게 동작하는지, 로그가 아니라
DB 상태와 커넥션 ID로 직접 증명하는 실습 프로젝트입니다. 개념은 이미
[블로그 글](https://yoonxjoong.github.io/posts/spring-transaction-propagation-isolation/)에
정리해뒀지만, 그 글의 예시 코드는 실행 검증을 하지 않은 스니펫이었습니다 — 이 프로젝트는 이를 직접
실행해 검증합니다.

## 구성

- Spring Boot 3.3 + Spring Data JPA(Hibernate) + MySQL 8.0
- `Order`, `AuditLog` 두 엔티티만 있는 최소 도메인
- 트랜잭션 매니저 두 개: 기본 `JpaTransactionManager`(`transactionManager`)와, NESTED 실험용
  `DataSourceTransactionManager`(`nestedCapableTransactionManager`)

## 실행 방법

```bash
docker compose up -d
docker run --rm --network host \
  -v "$(pwd)":/app -v ~/.gradle-cache-shared:/home/gradle/.gradle \
  -w /app gradle:8-jdk21 gradle test --no-daemon
```

로컬에 Java/Gradle이 없어서, 캐시된 `gradle:8-jdk21` 이미지로 테스트를 실행했습니다. `--network host`로
컨테이너 안에서 `localhost:3316`의 `docker-compose` MySQL에 바로 접속합니다.

## 검증 결과

### REQUIRED — 부모/자식이 물리적으로 같은 커넥션

자식(`InventoryService.decreaseStock`, 기본값 REQUIRED)이 예외를 던지면 부모(`RequiredOrderService.placeOrder`)의
`Order` insert까지 통째로 롤백됩니다. `connection_id()`로 확인한 결과 부모 시작 시점, 자식 실행 시점,
자식 이후 부모 재개 시점의 커넥션 ID가 전부 동일했습니다 — 같은 물리 커넥션/트랜잭션을 공유한다는 것을
직접 확인했습니다.

### REQUIRES_NEW — 부모가 롤백돼도 자식은 이미 커밋된 채로 남음

`AuditLogService.record`를 REQUIRES_NEW로 호출한 뒤 부모가 의도적으로 실패하면, `Order`는 롤백되지만
`AuditLog`는 그대로 남습니다. 커넥션 ID를 비교하면 부모와 자식이 서로 다르고(별도 물리 커넥션), 자식이
끝난 뒤 부모는 원래 커넥션으로 복귀합니다 — 기존 트랜잭션을 suspend하고 새로 시작한다는 설명이 문서상의
표현이 아니라 실제 커넥션 수준에서 일어나는 일이라는 것을 확인했습니다.

### NESTED — JPA에서는 설정으로 활성화할 수 있는 옵션이 아니다

`JpaTransactionManager.setNestedTransactionAllowed(true)`만 켜주면 될 것으로 예상했지만, 실제로
실행하면 여전히 `NestedTransactionNotSupportedException: JpaDialect does not support savepoints`가
발생합니다.

원인은 spring-orm 6.1.13의 클래스 파일을 직접 분석해 확인했습니다.

- `JpaTransactionManager$JpaTransactionObject.getSavepointManager()`는 `EntityManagerHolder`에 저장된
  savepoint 매니저를 찾는데, 이 값은 `JpaDialect.beginTransaction()`의 반환값이 스프링의
  `SavepointManager` 인터페이스를 구현하고 있을 때만 채워집니다.
- `HibernateJpaDialect.beginTransaction()`은 `HibernateJpaDialect$SessionTransactionData`라는 객체를
  반환하는데, 이 클래스는 `SavepointManager`를 구현하지 않습니다.
- 즉 `nestedTransactionAllowed` 플래그는 필요조건일 뿐 충분조건이 아니며, **Hibernate 연동에는 애초에
  savepoint 매니저를 생성하는 경로 자체가 없습니다.** Hibernate 세션에서 JDBC 커넥션을 직접 꺼내
  `SavepointManager`를 구현하는 커스텀 `JpaDialect`를 만들지 않는 한, 설정 몇 줄로 해결할 문제가
  아니었습니다.

그래서 NESTED가 실제로 동작하는 경로는 JPA가 아니라 **순수 JDBC**(`DataSourceTransactionManager` +
`JdbcTemplate`)로 전환해 확인했습니다.

- 기본 `JpaTransactionManager`로 NESTED 시도 → `NestedTransactionNotSupportedException` (예상대로 실패)
- `DataSourceTransactionManager`로 NESTED 시도 → 나쁜 아이템 하나는 savepoint까지만 롤백되고, 좋은
  아이템과 배치 자체는 정상 커밋됨
- 같은 배치에서 **부모가 최종적으로 롤백되면**, savepoint를 이미 통과했던 "좋은 아이템"도 함께
  사라짐 — REQUIRES_NEW(부모 롤백과 무관하게 살아남음)와 정반대라는 것을 같은 테스트 안에서 대조로
  확인했습니다.

MySQL로 전환할 때는 InnoDB 스토리지 엔진(기본값)인지만 확인하면 됩니다 — savepoint는 InnoDB에서만
지원되고 MyISAM에서는 트랜잭션 자체가 지원되지 않습니다.

### self-invocation — 예외 없이 조용히 트랜잭션 보호만 사라짐

같은 클래스 안에서 `this.method()`로 `@Transactional(REQUIRES_NEW)` 메서드를 호출하면 프록시를
거치지 않아 트랜잭션이 시작되지 않는다는 것은 알려진 내용입니다. 여기서는
`TransactionSynchronizationManager.isActualTransactionActive()`로 이를 값으로 증명하고, 한 걸음 더
나아가 그 상태로 실제 저장을 호출하면 어떤 일이 일어나는지 확인했습니다.

`TransactionRequiredException`과 같은 예외가 즉시 발생할 것으로 예상했으나, 실제로는 **예외 없이
저장이 완료됩니다.** Hibernate는 활성 트랜잭션이 없으면 사실상 autocommit과 같은 방식으로 세션을 즉시
반영하기 때문입니다. 즉 self-invocation의 위험은 에러로 바로 드러나는 종류가 아니라, **롤백 보호만
소리 없이 사라지는** 종류입니다 — 에러 로그도 없이 정상 동작처럼 보이기 때문에 더 위험합니다.

### 롤백 규칙 — checked exception은 기본값으로 롤백 안 됨

`@Transactional` 기본 설정에서 checked exception(`IOException`)을 던지면 `Order`가 그대로 커밋되고,
`rollbackFor = Exception.class`를 명시해야 롤백됩니다. unchecked exception은 기본 설정으로도
롤백됩니다. 문서에 있는 내용과 일치했습니다.

## 한계 및 남는 궁금증

- NESTED용 커스텀 `JpaDialect`(Hibernate 세션에서 JDBC 커넥션을 직접 꺼내 `SavepointManager`를 구현)는
  만들지 않았습니다. 이론적으로는 가능해 보이지만, 실무에서 NESTED를 JPA와 결합해야 할 필요성이 크지
  않다고 판단해 이번 범위에서 제외했습니다.
- self-invocation으로 트랜잭션 없이 저장될 때 Hibernate 세션이 정확히 어떤 방식으로 동작하는지(순수
  autocommit인지, flush마다 암묵적 트랜잭션을 여닫는 것인지)는 추가로 확인하지 않았습니다.
