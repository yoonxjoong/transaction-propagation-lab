package com.example.txlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.txlab.repository.OrderRepository;
import com.example.txlab.service.NestedBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.NestedTransactionNotSupportedException;

@SpringBootTest
class NestedPropagationTest {

    @Autowired
    private NestedBatchService nestedBatchService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void 스프링부트_기본_JpaTransactionManager는_NESTED를_지원하지_않는다() {
        assertThatThrownBy(nestedBatchService::runWithDefaultManager)
                .isInstanceOf(NestedTransactionNotSupportedException.class);
    }

    @Test
    void nestedTransactionAllowed를_켜면_자식_실패는_savepoint까지만_롤백되고_배치는_커밋된다() {
        nestedBatchService.runWithCapableManager(false);

        assertThat(orderRepository.count()).isEqualTo(2);
        assertThat(orderRepository.findAll())
                .extracting(order -> order.getProductName())
                .containsExactlyInAnyOrder("nested-outer-capable", "nested-good-item");
    }

    @Test
    void 부모_트랜잭션이_최종적으로_롤백되면_savepoint를_통과한_NESTED_자식도_함께_사라진다() {
        assertThatThrownBy(() -> nestedBatchService.runWithCapableManager(true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(orderRepository.count())
                .as("REQUIRES_NEW와 달리 NESTED는 부모와 물리적으로 같은 트랜잭션이라, 부모가 롤백되면 이미 savepoint를 통과했던 자식도 함께 사라져야 한다")
                .isZero();
    }
}
