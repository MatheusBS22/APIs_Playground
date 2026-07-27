package MatheusAPI.s.AI_FinanceApp.user;

import MatheusAPI.s.AI_FinanceApp.group.Group;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
@Entity
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //I'll change for and UUID object in the future

    @Enumerated(EnumType.STRING)
    private AccType accType;

    @ElementCollection(targetClass = AccPermissions.class)
    @Enumerated(EnumType.STRING)
    private Set<AccPermissions> accPermissions;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private Group group;

    private String username;
    private String surname;

    //Constructor WithAll and Without args for Jakarta
    public UserAccount() {
    }

    public UserAccount(
            AccType accType,
            Set<AccPermissions> accPermissions,
            String username,
            String surname) {
        this.accType = accType;
        this.username = username;
        this.surname = surname;
        this.accPermissions = accPermissions;
    }

    public UserAccount(AccType accType, String username, String surname) {
        this.accType = accType;
        this.username = username;
        this.surname = surname;
        this.accPermissions = accType.getDefaultPermissions();
    }
}
