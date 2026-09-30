package com.example.txlab.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * REQUIRED/REQUIRES_NEW가 물리적으로 같은 커넥션을 쓰는지 다른 커넥션을 쓰는지를
 * pg_backend_pid()로 직접 확인하기 위한 도구. 트랜잭션 매니저가 주장하는 동작이
 * 실제 DB 세션에도 그대로 반영되는지 증명하는 용도.
 */
@Component
public class ConnectionIdProbe {

    @PersistenceContext
    private EntityManager entityManager;

    public Integer currentBackendPid() {
        Object result = entityManager.createNativeQuery("select pg_backend_pid()").getSingleResult();
        return ((Number) result).intValue();
    }
}
