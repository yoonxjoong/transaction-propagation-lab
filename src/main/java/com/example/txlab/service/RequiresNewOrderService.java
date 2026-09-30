package com.example.txlab.service;

import com.example.txlab.domain.Order;
import com.example.txlab.repository.OrderRepository;
import com.example.txlab.support.ConnectionIdProbe;
import com.example.txlab.support.TxTrace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequiresNewOrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ConnectionIdProbe probe;

    @Transactional // REQUIRED (기본값)
    public void placeOrder(TxTrace trace, boolean failAfterAudit) {
        trace.record("outer-before", probe.currentBackendPid());
        orderRepository.save(new Order("requires-new-demo"));
        auditLogService.record(trace, "placeOrder attempted"); // REQUIRES_NEW
        trace.record("outer-after", probe.currentBackendPid());
        if (failAfterAudit) {
            throw new IllegalStateException("주문 처리 중 실패");
        }
    }
}
