package com.example.txlab;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.txlab.repository.AuditLogRepository;
import com.example.txlab.service.SelfInvocationDemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SelfInvocationTest {

    @Autowired
    private SelfInvocationDemoService selfInvocationDemoService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void 프록시를_거쳐_직접_호출하면_REQUIRES_NEW가_실제로_트랜잭션을_시작시킨다() {
        boolean active = selfInvocationDemoService.isActualTransactionActive();

        assertThat(active).isTrue();
    }

    @Test
    void 같은_클래스_내부에서_this로_호출하면_프록시를_거치지_않아_Transactional이_무시된다() {
        boolean active = selfInvocationDemoService.callTransactionalMethodViaSelfInvocation();

        assertThat(active).isFalse();
    }

    @Test
    void self_invocation은_예외_없이_트랜잭션_보호_없는_상태로_조용히_저장된다() {
        // 직접 돌려보기 전엔 TransactionRequiredException이 날 거라 예상했지만,
        // 실제로는 예외 없이 저장된다 — Hibernate가 활성 트랜잭션이 없으면
        // autocommit 성격의 세션으로 즉시 반영해버리기 때문. 즉 self-invocation의
        // 위험은 "에러가 나서 바로 들키는" 게 아니라 "조용히 롤백 보호만 사라지는" 쪽이다.
        selfInvocationDemoService.saveAuditViaSelfInvocation();

        assertThat(auditLogRepository.count()).isEqualTo(1);
    }
}
