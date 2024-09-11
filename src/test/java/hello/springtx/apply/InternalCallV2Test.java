package hello.springtx.apply;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InternalCallV2Test {

    @Autowired
    CallService callService;

    @Test
    void printProxy() {
        log.info("callService class={}", callService.getClass());
    }

    @Test
    void externalCallV2() {
        callService.external();
    }

    @TestConfiguration
    static class internalCallV2TestConfig {

        @Bean
        CallService callService() {
            return new CallService(internalService());
        }

        @Bean
        InternalService internalService() {
            return new InternalService();
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    static class CallService {

        private final InternalService internalService;

        public void external() { // 외부에서 호출하는 메서드 (spring Bean 으로 등록이 안되있기 떄문에 프록시 생성이 안된다.) 트랜잭션은 수행 된다.
            log.info("call external");
            printTxInfo();
            internalService.internal(); // 별도로 분리된 internalService 를 통해 internal() 를 호출하면 트랜잭션과 프록시가 적용 된다.
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive(); // 트랜잭션이 적용 되었는지 여부를 확인하는 메서드
            log.info("active={}", txActive);
        }
    }

    // InternalService 를 '별도의 클래스로 분리'해서 해당 프록시를 외부에서 호출할 수 있도록 설정
        static class InternalService {

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

