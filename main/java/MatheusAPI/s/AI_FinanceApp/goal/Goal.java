package MatheusAPI.s.AI_FinanceApp.goal;

import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "t_goal")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull
    private String name;

    @Column(nullable = false)
    @NotNull
    private BigDecimal targetAmount;

    @Column(nullable = false)
    @NotNull
    private BigDecimal currentAmount;

    @Column(nullable = false)
    @NotNull
    private LocalDate deadline;

    @Column(nullable = false, updatable = false)
    @NotNull
    private LocalDate createdAt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private Group group;

    // null = meta do grupo inteiro (compartilhada); preenchido = meta individual desse usuário
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    private UserAccount owner;

    public Goal() {
        this.createdAt = LocalDate.now();
        this.currentAmount = BigDecimal.ZERO;
    }
}
