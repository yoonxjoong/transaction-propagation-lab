package com.example.txlab.service;

import com.example.txlab.domain.AuditLog;
import com.example.txlab.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SelfInvocationDemoService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * 프록시를 거쳐서(=외부에서 이 빈을 통해) 호출됐는지 확인하기 위한 메서드.
     * 정상적으로 프록시를 거치면 REQUIRES_NEW가 실제로 트랜잭션을 시작시킨다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean isActualTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /**
     * 같은 클래스 안에서 this.isActualTransactionActive()를 직접 호출 —
     * 프록시를 거치지 않으므로 @Transactional(REQUIRES_NEW)가 무시되고
     * 트랜잭션이 시작되지 않을 것으로 예상된다.
     */
    public boolean callTransactionalMethodViaSelfInvocation() {
        return this.isActualTransactionActive();
    }

    /**
     * 트랜잭션이 없는 채로 리포지토리 save를 호출하면 실제로 무슨 일이
     * 일어나는지 확인하기 위한 메서드 — 조용히 성공하는지, 예외가 나는지는
     * 직접 돌려보기 전까지는 모른다.
     */
    public void saveAuditViaSelfInvocation() {
        this.saveAuditRequiresNew();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditRequiresNew() {
        auditLogRepository.save(new AuditLog("self-invocation"));
    }
}
