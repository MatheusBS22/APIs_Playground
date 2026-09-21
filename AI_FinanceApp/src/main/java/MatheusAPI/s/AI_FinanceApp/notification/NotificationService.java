package MatheusAPI.s.AI_FinanceApp.notification;

import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.user.AccType;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import MatheusAPI.s.AI_FinanceApp.user.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;

    // ---------- criação (chamada pelos outros services, nunca direto por um endpoint) ----------

    @Transactional
    public void notify(UserAccount recipient, NotificationType type, String message) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    // Avisa todo mundo da lista, menos quem já sabe (ex: quem fez a ação, ou o novo membro que acabou de entrar).
    @Transactional
    public void notifyOthers(List<UserAccount> recipients, Long excludeUserId, NotificationType type, String message) {
        for (UserAccount recipient : recipients) {
            if (recipient.getId().equals(excludeUserId)) continue;
            notify(recipient, type, message);
        }
    }

    // ---------- leitura (endpoints) ----------

    public List<Notification> list(Long userId, Long requesterId) {
        requireSelfOrDeveloper(userId, requesterId);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    public Map<String, Long> unreadCount(Long userId, Long requesterId) {
        requireSelfOrDeveloper(userId, requesterId);
        return Map.of("unreadCount", notificationRepository.countByRecipientIdAndReadFalse(userId));
    }

    @Transactional
    public void markRead(Long id, Long requesterId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aviso não encontrado: " + id));
        requireSelfOrDeveloper(notification.getRecipient().getId(), requesterId);
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(Long userId, Long requesterId) {
        requireSelfOrDeveloper(userId, requesterId);
        List<Notification> unread = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> !n.isRead())
                .toList();
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    // ---------- helpers ----------

    // Formata um valor decimal como "R$ 1.234,56", igual ao front, pra a mensagem do aviso já vir pronta.
    public static String formatCurrency(java.math.BigDecimal amount) {
        java.math.BigDecimal scaled = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        String plain = scaled.toPlainString();
        boolean negative = plain.startsWith("-");
        if (negative) plain = plain.substring(1);
        String[] parts = plain.split("\\.");
        String intPart = parts[0];
        String decPart = parts.length > 1 ? parts[1] : "00";
        StringBuilder withThousands = new StringBuilder();
        int count = 0;
        for (int i = intPart.length() - 1; i >= 0; i--) {
            withThousands.append(intPart.charAt(i));
            count++;
            if (count % 3 == 0 && i != 0) withThousands.append('.');
        }
        return (negative ? "-R$ " : "R$ ") + withThousands.reverse() + "," + decPart;
    }

    private void requireSelfOrDeveloper(Long targetUserId, Long requesterId) {
        UserAccount requester = userAccountRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + requesterId));
        if (requester.getAccType() == AccType.DEVELOPER) return;
        if (!targetUserId.equals(requesterId)) {
            throw new AccessDeniedException("Você não pode ver os avisos de outro usuário");
        }
    }
}
