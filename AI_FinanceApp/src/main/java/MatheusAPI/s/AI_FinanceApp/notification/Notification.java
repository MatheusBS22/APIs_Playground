package MatheusAPI.s.AI_FinanceApp.notification;

import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_notification")
@Getter @Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private UserAccount recipient;

    @Enumerated(EnumType.STRING)
    @NotNull
    private NotificationType type;

    @Column(nullable = false, length = 255)
    @NotNull
    private String message;

    @Column(nullable = false, updatable = false)
    @NotNull
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean read = false;

    public Notification() {
        this.createdAt = LocalDateTime.now();
    }
}
