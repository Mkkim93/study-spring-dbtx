package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commit() {
        log.info("트랜잭션 시작");
        // DefaultTransactionAttribute 는 기본 트랜잭션 속성을 나타내며, 기본적으로 전파 방식(propagation behavior)은 REQUIRED 로 설정되어 있습니다.
        // 이 속성으로 트랜잭션을 정의한 후 트랜잭션을 시작할 수 있습니다.
        // REQUIRED : 현재 트랜잭션의 상태를 확인하고 트랜잭션이 있으면 해당 트랜잭션을 사용하고 없으면 새로운 트랜잭션을 생성하여 사용한다.
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);
        log.info("트랜잭션 커밋 완료");
    }

    @Test
    void rollback() {
        log.info("트랜잭션 롤백");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);
        log.info("트랜잭션 롤백 완료");
    }

    @Test
    void double_commit() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 커밋");
        txManager.commit(tx2);
    }

    @Test
    void double_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 롤백");
        txManager.rollback(tx2);
    }

    @Test
    void inner_commit() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction()); // isNew.. : 처음 시작 하는 트랜잭션 인지 확인 (true)

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute()); // 내부 트랜잭션이 외부 트랜잭션에 참여하여 하나의 물리 트랜잭션이 되는 시점
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction()); // isNew.. (false)

        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner); // 신규 트랜잭션이 아니기때문에 커밋이 안된다.

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
    }

    @Test
    void outer_rollback() {
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction()); // isNew.. : 처음 시작 하는 트랜잭션 인지 확인 (true)

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute()); // 내부 트랜잭션이 외부 트랜잭션에 참여하여 하나의 물리 트랜잭션이 되는 시점
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction()); // isNew.. (false)

        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner); // 신규 트랜잭션이 아니기때문에 커밋이 안된다.

        log.info("외부 트랜잭션 롤백"); // Initiating transaction rollback : 시작(isNew.. true) 트랜잭션 롤백
        txManager.rollback(outer); // 실제 물리 트랜잭션 커밋 : 가장 첫번째 시작하는 트랜잭션이기때문에 커밋 (= 물리트랜잭션의 본체)
    }

    @Test
    void inner_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction()); // isNew.. : 처음 시작 하는 트랜잭션 인지 확인 (true)

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute()); // 내부 트랜잭션이 외부 트랜잭션에 참여하여 하나의 물리 트랜잭션이 되는 시점
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction()); // isNew.. (false)

        log.info("내부 트랜잭션 롤백"); // Participating transaction failed - marking existing transaction as rollback-only
        // 내부(inner) 트랜잭션이 rollback 을 호출하면 현재 물리 트랜잭션에 rollback 한다는 marking 같은 것을 한다. 이것을 외부 트랜잭션이 가지고 있다.
        txManager.rollback(inner);

        log.info("외부 트랜잭션 커밋"); // Initiating transaction rollback : 시작(isNew.. true) 트랜잭션 롤백
        Assertions.assertThatThrownBy(() ->txManager.commit(outer))
                .isInstanceOf(UnexpectedRollbackException.class); // Global transaction is marked as rollback-only but transactional code requested commit 어딘가 논리 트랜잭션 중에 롤백이 되었다
        // 그래서 이 트랜잭션은 롤백이 되어야 한다.

        // 즉, 외부트랜잭션이 내부의 논리트랜잭션의 롤백때문에 롤백이 되어야 하는데 커밋을 하려고 하니까 서로 충돌되서 예외가 발생했다.
    }

    @Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction()); // isNew.. : 처음 시작 하는 트랜잭션 인지 확인 (true)

        innerLogic();

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
    }

    private void innerLogic() {
        log.info("내부 트랜잭션 시작");
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); // REQUIRES_NEW : 새로운 트랜잭션 영역으로 설정해준다.
        TransactionStatus inner = txManager.getTransaction(definition); // 새로운 트랜잭션으로 설정된 definition 인스턴스변수를 inner 트랜잭션으로 초기화한다. (새로운 커넥션 생성)
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction()); // true

        // 외부 트랜잭션과 내부 트랜잭션의 별도
        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner);
    }
}