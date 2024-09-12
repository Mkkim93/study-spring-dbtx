package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired LogRepository logRepository;

    /**
     * memberService @Transactional: OFF
     * memberRepository @Transactional: ON
     * logRepository @Transactional: ON
     */

    @Test
    void outerTxOff_success() {
        String userName = "outerTxOff_success";

        memberService.joinV1(userName);

        assertTrue(memberRepository.find(userName).isPresent()); // 데이터가 참이어야 한다
        assertTrue(logRepository.find(userName).isPresent());
    }

    @Test
    void outerTxOff_fail() {
        String userName = "로그예외_outerTxOff_fail"; // 로그예외를 전달하면 RuntimeException 이 발생 (rollback)

        assertThatThrownBy(() -> memberService.joinV1(userName))
                .isInstanceOf(RuntimeException.class);

        assertTrue(memberRepository.find(userName).isPresent()); // memberRepository 트랜잭션 commit
        assertTrue(logRepository.find(userName).isEmpty()); // logRepository 트랜잭션 rollback - runtimeException 발생
    }

    /**
     * memberService @Transactional: ON
     * memberRepository @Transactional: OFF
     * logRepository @Transactional: OFF
     */
    @Test
    void singleTx() {
        String userName = "outerTxOff_success";

        memberService.joinV1(userName);

        assertTrue(memberRepository.find(userName).isPresent()); // 데이터가 참이어야 한다
        assertTrue(logRepository.find(userName).isPresent());
    }

    /**
     * memberService @Transactional: ON
     * memberRepository @Transactional: ON
     * logRepository @Transactional: ON
     */
    @Test
    void outerTxOn_success() {
        String userName = "outerTxOn_success";

        memberService.joinV1(userName);

        assertTrue(memberRepository.find(userName).isPresent()); // 데이터가 참이어야 한다
        assertTrue(logRepository.find(userName).isPresent());
    }

    /**
     * memberService @Transactional: ON
     * memberRepository @Transactional: ON
     * logRepository @Transactional: ON
     */
    @Test
    void outerTxOn_fail() { // Initiating transaction rollback : 모든 데이터 rollback
        String userName = "로그예외_outerTxOn_fail"; // 로그예외를 전달하면 RuntimeException 이 발생 (rollback)

        assertThatThrownBy(() -> memberService.joinV1(userName))
                .isInstanceOf(RuntimeException.class);
        // 논리 트랜잭션이 하나라도 rollback 되면 물리 트랜잭션도 rollback 수행
        assertTrue(memberRepository.find(userName).isEmpty()); // memberRepository 트랜잭션에는 데이터 x (.isEmpty() : true)
        assertTrue(logRepository.find(userName).isEmpty()); // logRepository 트랜잭션 rollback - runtimeException 발생
    }

    /**
     * memberService @Transactional: ON
     * memberRepository @Transactional: ON
     * logRepository @Transactional: ON
     */
    @Test
    void recoverException_fail() {
        String userName = "로그예외_recoverException_fail"; // 로그예외를 전달하면 RuntimeException 이 발생 (rollback)

        assertThatThrownBy(() -> memberService.joinV2(userName))
                .isInstanceOf(UnexpectedRollbackException.class);

        // 논리 트랜잭션이 하나라도 rollback 되면 물리 트랜잭션도 rollback 수행
        assertTrue(memberRepository.find(userName).isEmpty()); // memberRepository 트랜잭션에는 데이터 x (.isEmpty() : true)
        assertTrue(logRepository.find(userName).isEmpty()); // logRepository 트랜잭션 rollback - runtimeException 발생
    }

    /**
     * memberService @Transactional: ON
     * memberRepository @Transactional: ON
     * logRepository @Transactional: ON (REQUIRED_NEW) Exception
     */
    @Test
    void requiredException_success() { // 물리트랜잭션 2개로 동작 (TS1.MEMBER / TS2.LOG)
        String userName = "로그예외_requiredException_success"; // 로그예외를 전달하면 RuntimeException 이 발생 (rollback)

        memberService.joinV2(userName);

        // 물리트랜잭션이 1개일 때 내부 논리트랜잭션 한쪽이 ROLLBACK 이 수행되면 모든 물리트랜잭션 내에 있는 논리 트랜잭션도 ROLLBACK 이 되야한다 즉, 모든 물리 트랜잭션 ROLLBACK 이 수행
        // 물리트랜잭션 2개 일때 한쪽은 COMMIT, 한쪽은 ROLLBACK 이 가능함
        assertTrue(memberRepository.find(userName).isPresent()); // 회원 로직은 COMMIT
        assertTrue(logRepository.find(userName).isEmpty()); // LOG 로직은 ROLLBACK
        // 가능한 이유 : LogRepository 의 트랜잭션의 옵션을 REQUIRED_NEW 를 통해 새로운 트랜잭션으로 설정하였기 때문
        // 하나의 물리 트랜잭션 내, 논리트랜잭션 중에서 롤백이 수행되면 rollback_only marker 가 적용되기 때문에 전체 물리트랜잭션도 rollback 이 되어야 하지만
        // logRepository 에서 물리트랜잭션을 새로 생성 하였기 때문에 위의 로직은 물리 트랜잭션 2개가 동작하고 하나의 물리 트랜잭션만 rollback 이 가능하게 된다.
    }
}