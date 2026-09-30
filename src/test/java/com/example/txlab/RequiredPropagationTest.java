package com.example.txlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.txlab.repository.AuditLogRepository;
import com.example.txlab.repository.OrderRepository;
import com.example.txlab.service.RequiredOrderService;
import com.example.txlab.support.TxTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RequiredPropagationTest {

    @Autowired
    private RequiredOrderService requiredOrderService;

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
    void 자식이_실패하면_REQUIRED로_참여한_부모도_함께_롤백된다() {
        TxTrace trace = new TxTrace();

        assertThatThrownBy(() -> requiredOrderService.placeOrder(trace, true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void REQUIRED는_부모_자식이_물리적으로_같은_커넥션을_공유한다() {
        TxTrace trace = new TxTrace();

        requiredOrderService.placeOrder(trace, false);

        assertThat(orderRepository.count()).isEqualTo(1);
        int outerBefore = trace.pidOf("outer-before");
        int innerRequired = trace.pidOf("inner-required");
        int outerAfter = trace.pidOf("outer-after");
        assertThat(innerRequired).as("REQUIRED로 참여한 자식은 부모와 같은 커넥션 pid를 써야 한다").isEqualTo(outerBefore);
        assertThat(outerAfter).as("자식 호출 이후에도 부모는 같은 커넥션을 그대로 쓴다").isEqualTo(outerBefore);
    }
}
