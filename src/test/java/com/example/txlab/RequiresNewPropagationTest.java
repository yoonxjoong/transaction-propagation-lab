package com.example.txlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.txlab.repository.AuditLogRepository;
import com.example.txlab.repository.OrderRepository;
import com.example.txlab.service.RequiresNewOrderService;
import com.example.txlab.support.TxTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RequiresNewPropagationTest {

    @Autowired
    private RequiresNewOrderService requiresNewOrderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
        auditLogRepository.deleteAll();
    }

    @Test
    void 부모가_나중에_롤백돼도_REQUIRES_NEW로_처리한_자식은_이미_커밋된_채로_남는다() {
        TxTrace trace = new TxTrace();

        assertThatThrownBy(() -> requiresNewOrderService.placeOrder(trace, true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(orderRepository.count())
                .as("부모 트랜잭션의 Order는 롤백돼서 없어야 한다")
                .isZero();
        assertThat(auditLogRepository.count())
                .as("REQUIRES_NEW로 처리된 AuditLog는 독립적으로 이미 커밋되어 남아있어야 한다")
                .isEqualTo(1);
    }

    @Test
    void REQUIRES_NEW는_부모와_물리적으로_다른_커넥션을_쓰고_끝나면_부모_커넥션으로_복귀한다() {
        TxTrace trace = new TxTrace();

        requiresNewOrderService.placeOrder(trace, false);

        int outerBefore = trace.pidOf("outer-before");
        int innerRequiresNew = trace.pidOf("inner-requires-new");
        int outerAfter = trace.pidOf("outer-after");

        assertThat(innerRequiresNew)
                .as("REQUIRES_NEW는 부모 커넥션을 suspend하고 별도 물리 커넥션을 써야 한다")
                .isNotEqualTo(outerBefore);
        assertThat(outerAfter)
                .as("자식이 끝나면 부모는 자신의 원래 커넥션으로 resume되어야 한다")
                .isEqualTo(outerBefore);
    }
}
