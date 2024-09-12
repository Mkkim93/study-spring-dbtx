package hello.springtx.propagation;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final EntityManager em;

     @Transactional
    public void save(Member member) {
        log.info("member 저장");
        em.persist(member);
    }

    // jpql 은 대소문자를 구분한다 from member X / from Member O
    // 예외 발생 : org.springframework.dao.InvalidDataAccessApiUsageException: org.hibernate.query.sqm.UnknownEntityException: Could not resolve root entity 'member'
    public Optional<Member> find(String userName) {
        return em.createQuery("select m from Member m where m.userName= :userName", Member.class)
                .setParameter("userName", userName)
                .getResultList().stream().findAny();
    }
}
