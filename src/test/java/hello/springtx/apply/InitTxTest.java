package hello.springtx.apply;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
public class InitTxTest {

    @Autowired
    Bye bye;
    @Autowired
    Hello hello;

    @Test
    void go() {
        // 초기화 코드는 스프링이 초기화 시점에 호출된다 (@PostConstruct 가 적용된 메서드) 즉, initV1() 을 호출해준다. (신기..)
        // @PostConstruct 의 호출 우선 순위는 @Bean 이 등록된 순서를 기준으로 한다.

        // 현재 @Bean 에 등록되는 순서에 따라 go() 메서드에서 출력 우선순위가 결정된다.
        // @PostConstruct 를 두개 가지고 있는 Hello 클래스에서 우선순위는 스프링이 보장하지 않는다.
        // initV1 과 initV3 의 한쪽 로직이 길어지더라도 go() 메서드에서 호출되는 시점은 랜덤이다.
        // 호출 시점을 맞추기 위해서는 스프링에서 제공하는 EventListener 또는 ApplicationEventListener 를 사용하자
        hello.initV2();
    }

    @TestConfiguration
    static class InitTxTestConfig {

        @Bean
        Bye bye() {
            return new Bye();
        }

        @Bean
        Hello hello() {
            return new Hello();
        }
    }

    @Slf4j
    static class Bye {

        @PostConstruct
        @Transactional
        public void initV4() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello initV4 @PostConstruct tx active={}", isActive);
        }
    }

    @Slf4j
    static class Hello {

        @PostConstruct // 종속성 주입이 완료된 후 초기화를 수행하기 위해 실행해야 하는 메서드에 사용 (?)
        @Transactional
        public void initV1() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello initV1 @PostConstruct tx active={}", isActive);
        }

        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void initV2() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello initV2 @ApplicationReadyEvent tx active={}", isActive);
        }

        @PostConstruct
        public void initV3() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello initV3 @PostConstruct tx active={}", isActive);
        }
    }
}
