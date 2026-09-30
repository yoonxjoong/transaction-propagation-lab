package com.example.txlab.service;

import com.example.txlab.domain.Order;
import com.example.txlab.repository.OrderRepository;
import com.example.txlab.support.ConnectionIdProbe;
import com.example.txlab.support.TxTrace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequiredOrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ConnectionIdProbe probe;

    @Transactional // REQUIRED (기본값)
    public void placeOrder(TxTrace trace, boolean failInInventory) {
        trace.record("outer-before", probe.currentBackendPid());
        orderRepository.save(new Order("required-demo"));
        inventoryService.decreaseStock(trace, failInInventory);
        trace.record("outer-after", probe.currentBackendPid());
    }
}
