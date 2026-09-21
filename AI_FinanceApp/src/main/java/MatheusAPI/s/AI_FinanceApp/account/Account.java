package MatheusAPI.s.AI_FinanceApp.account;

import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_account")
@Getter @Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    @NotNull
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private UserAccount creator;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private Group group;

    @Column(nullable = false, updatable = false)
    @NotNull
    private LocalDateTime createdAt;

}
