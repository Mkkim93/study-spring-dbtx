package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Call;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InternalCallV1Test {

    @Autowired
    CallService callService;

    @Test
    void printProxy() {
        log.info("callService class={}", callService.getClass());
    }

    @Test
    void internalCall() {
        callService.internal();
    }

    @Test
    void externalCall() {
        callService.external();
    }

    @TestConfiguration
    static class internalCallV1TestConfig {

        @Bean
        CallService callService() {
            return new CallService();
        }
    }

    @Slf4j
    static class CallService {

        public void external() { // 외부에서 호출하는 메서드 (spring Bean 으로 등록이 안되있기 떄문에 프록시 생성이 안된다.) 트랜잭션은 수행 된다.
            log.info("call external");
            printTxInfo();
            internal(); // this.internal(); -> 프록시를 거치지 않은 internal() 순수 자신의 메서드
        }

        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive(); // 트랜잭션이 적용 되었는지 여부를 확인하는 메서드
            log.info("active={}", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly(); // 트랜잭션이 readOnly 인지 확인하는 메서드
            log.info("tx readOnly={}", readOnly);
        }
    }
}
