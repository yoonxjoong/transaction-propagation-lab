package com.example.txlab.service;

import com.example.txlab.support.ConnectionIdProbe;
import com.example.txlab.support.TxTrace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    @Autowired
    private ConnectionIdProbe probe;

    @Transactional // REQUIRED (기본값)
    public void decreaseStock(TxTrace trace, boolean fail) {
        trace.record("inner-required", probe.currentBackendPid());
        if (fail) {
            throw new IllegalStateException("재고 부족");
        }
    }
}
