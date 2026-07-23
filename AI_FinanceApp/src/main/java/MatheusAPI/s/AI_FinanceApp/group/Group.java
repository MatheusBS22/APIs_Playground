package MatheusAPI.s.AI_FinanceApp.group;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_group")
@Getter @Setter
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    @NotNull
    private String name;

    @Column(nullable = false, updatable = false)
    @NotNull
    private LocalDateTime createdAt;

}
