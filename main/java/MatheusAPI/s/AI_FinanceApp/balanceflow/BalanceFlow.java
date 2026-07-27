package MatheusAPI.s.AI_FinanceApp.balanceflow;

import MatheusAPI.s.AI_FinanceApp.account.Account;
import MatheusAPI.s.AI_FinanceApp.category.Category;
import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_balance_flow")
@Getter @Setter
public class BalanceFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @NotNull
    private FlowType type; // EXPENSE ou INCOME

    @Column(nullable = false)
    @NotNull
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    private Category category;

    @Column(length = 32)
    private String title;

    @Column(length = 1024)
    private String notes;

    @Column(nullable = false)
    @NotNull
    private LocalDateTime createTime;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private UserAccount creator;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private Group group;

}
