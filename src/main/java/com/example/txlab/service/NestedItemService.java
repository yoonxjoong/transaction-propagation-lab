package com.example.txlab.service;

import com.example.txlab.domain.Order;
import com.example.txlab.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NestedItemService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 스프링 부트가 자동 구성하는 기본 "transactionManager"(JpaTransactionManager)
     * 빈으로 NESTED를 쓰면 어떻게 되는지 확인하기 위한 메서드 — Hibernate 연동에는
     * savepoint 매니저를 만들어주는 경로 자체가 없어서 호출 즉시
     * NestedTransactionNotSupportedException이 던져진다.
     */
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.NESTED)
    public void saveItemWithDefaultManager(String name) {
        orderRepository.save(new Order(name));
    }

    /**
     * DataSourceTransactionManager(순수 JDBC)로 NESTED를 쓰는 경로 — 진짜
     * JDBC savepoint API를 타므로 실제로 savepoint까지만 롤백된다. JPA
     * 리포지토리 대신 JdbcTemplate을 직접 쓰는 이유는, NESTED savepoint 관리가
     * JpaTransactionManager가 아니라 JDBC 커넥션 레벨에서만 지원되기 때문이다.
     */
    @Transactional(transactionManager = "nestedCapableTransactionManager", propagation = Propagation.NESTED)
    public void saveItemWithCapableManager(String name, boolean fail) {
        jdbcTemplate.update("insert into orders(product_name) values (?)", name);
        if (fail) {
            throw new IllegalStateException("아이템 저장 실패: " + name);
        }
    }
}
