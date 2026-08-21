package MatheusAPI.s.AI_FinanceApp.category;

import MatheusAPI.s.AI_FinanceApp.balanceflow.FlowType;
import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_category")
@Getter @Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    @NotNull
    private String name;

    @Enumerated(EnumType.STRING)
    @NotNull
    private FlowType type;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private Group group;

    // null = categoria do grupo (todos usam, só manager/dev gerencia).
    // preenchido = categoria pessoal (só o dono usa e gerencia).
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    private UserAccount owner;

}
