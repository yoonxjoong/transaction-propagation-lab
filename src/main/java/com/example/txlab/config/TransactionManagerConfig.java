package com.example.txlab.config;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransactionManagerConfig {

    /**
     * 트랜잭션 매니저 빈을 하나라도 직접 등록하면, 스프링 부트가 자동 구성하던
     * 기본 "transactionManager" 빈은 @ConditionalOnMissingBean(TransactionManager.class)
     * 조건 때문에 조용히 사라진다. 그래서 NESTED 실험용 매니저 하나만 추가하면 기존에
     * 잘 동작하던 평범한 @Transactional들까지 "No bean named 'transactionManager'"
     * 에러로 전부 깨진다 — 두 매니저를 모두 직접 선언하고 기본값을 @Primary로
     * 지정해야 원래 동작이 유지된다.
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    /**
     * nestedTransactionAllowed=true로 바꿔도 JpaTransactionManager로는 NESTED가
     * 끝내 동작하지 않는다 — 바이트코드로 직접 까본 결과, savepoint 매니저는
     * JpaDialect.beginTransaction()이 돌려주는 객체가 스프링의 SavepointManager
     * 인터페이스를 구현해야만 얻어지는데, HibernateJpaDialect가 반환하는
     * SessionTransactionData는 그 인터페이스를 구현하지 않는다. 즉 설정으로
     * 켤 수 있는 옵션이 아니라 Hibernate 연동 자체에 그 경로가 없는 것이다.
     * NESTED가 실제로 동작하는 건 JDBC 커넥션의 진짜 savepoint API를 쓰는
     * DataSourceTransactionManager 계열이라, 이 빈은 JPA가 아니라 순수 JDBC
     * (JdbcTemplate) 경로에만 사용한다.
     */
    @Bean
    public PlatformTransactionManager nestedCapableTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
