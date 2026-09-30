package com.example.txlab.service;

import com.example.txlab.domain.AuditLog;
import com.example.txlab.repository.AuditLogRepository;
import com.example.txlab.support.ConnectionIdProbe;
import com.example.txlab.support.TxTrace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ConnectionIdProbe probe;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(TxTrace trace, String message) {
        trace.record("inner-requires-new", probe.currentBackendPid());
        auditLogRepository.save(new AuditLog(message));
    }
}
