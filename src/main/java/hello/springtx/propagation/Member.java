package hello.springtx.propagation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Member {

    @Id @GeneratedValue
    private Long id;

    private String userName;

    public Member(){}

    public Member(String userName) {
        this.userName = userName;
    }
}

