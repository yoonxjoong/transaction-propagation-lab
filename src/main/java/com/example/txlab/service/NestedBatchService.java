package com.example.txlab.service;

import com.example.txlab.domain.Order;
import com.example.txlab.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NestedBatchService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NestedItemService nestedItemService;

    @Transactional // 기본 transactionManager(JPA), REQUIRED
    public void runWithDefaultManager() {
        orderRepository.save(new Order("nested-outer-default"));
        nestedItemService.saveItemWithDefaultManager("nested-child-default");
    }

    /**
     * 나쁜 아이템 하나가 savepoint까지만 롤백되고, 좋은 아이템과 배치 자체는
     * 정상 커밋되는 경로. failFinalCommit=true면 배치 마무리 단계에서 한 번 더
     * 실패시켜서, 이미 savepoint를 통과해 "성공"했던 아이템도 부모 물리
     * 트랜잭션 롤백에 함께 휩쓸리는지 확인한다. 순수 JDBC 매니저를 쓰므로
     * 이 메서드도 JdbcTemplate으로 통일한다.
     */
    @Transactional(transactionManager = "nestedCapableTransactionManager", propagation = Propagation.REQUIRED)
    public void runWithCapableManager(boolean failFinalCommit) {
        jdbcTemplate.update("insert into orders(product_name) values (?)", "nested-outer-capable");

        try {
            nestedItemService.saveItemWithCapableManager("nested-bad-item", true);
        } catch (RuntimeException ignored) {
            // savepoint까지만 롤백되고 배치는 계속 진행
        }

        nestedItemService.saveItemWithCapableManager("nested-good-item", false);

        if (failFinalCommit) {
            throw new IllegalStateException("배치 마무리 단계에서 실패 - 부모 전체 롤백");
        }
    }
}
