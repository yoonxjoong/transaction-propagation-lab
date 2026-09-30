package com.example.txlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.txlab.repository.OrderRepository;
import com.example.txlab.service.RollbackRuleService;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RollbackRuleTest {

    @Autowired
    private RollbackRuleService rollbackRuleService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void checked_exception은_기본값으로는_롤백되지_않는다() {
        assertThatThrownBy(() -> rollbackRuleService.saveThenThrowChecked("checked-demo"))
                .isInstanceOf(IOException.class);

        assertThat(orderRepository.count())
                .as("checked exception은 스프링 기본 롤백 규칙에 해당하지 않아 커밋된다")
                .isEqualTo(1);
    }

    @Test
    void rollbackFor를_명시하면_checked_exception도_롤백된다() {
        assertThatThrownBy(() -> rollbackRuleService.saveThenThrowCheckedWithRollbackFor("checked-rollbackfor-demo"))
                .isInstanceOf(IOException.class);

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void unchecked_exception은_기본값으로도_롤백된다() {
        assertThatThrownBy(() -> rollbackRuleService.saveThenThrowUnchecked("unchecked-demo"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(orderRepository.count()).isZero();
    }
}
