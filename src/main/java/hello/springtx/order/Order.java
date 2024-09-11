package hello.springtx.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import static jakarta.persistence.GenerationType.*;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id @GeneratedValue
    private Long id;

    private String userName; // 정상, 예외, 잔고 부족

    private String payStatus; // 상태 (대기, 완료)
}


