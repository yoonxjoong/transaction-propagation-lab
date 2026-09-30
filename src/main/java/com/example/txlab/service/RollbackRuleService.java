package com.example.txlab.service;

import com.example.txlab.domain.Order;
import com.example.txlab.repository.OrderRepository;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RollbackRuleService {

    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    public void saveThenThrowChecked(String name) throws IOException {
        orderRepository.save(new Order(name));
        throw new IOException("checked exception - 기본값으로는 롤백 안 됨");
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveThenThrowCheckedWithRollbackFor(String name) throws IOException {
        orderRepository.save(new Order(name));
        throw new IOException("checked exception - rollbackFor 명시했으니 롤백됨");
    }

    @Transactional
    public void saveThenThrowUnchecked(String name) {
        orderRepository.save(new Order(name));
        throw new IllegalStateException("unchecked exception - 기본값으로도 롤백됨");
    }
}
