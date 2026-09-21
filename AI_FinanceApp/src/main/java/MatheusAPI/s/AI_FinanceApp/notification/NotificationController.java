package MatheusAPI.s.AI_FinanceApp.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> list(@RequestParam Long requesterId) {
        return ResponseEntity.ok(notificationService.list(requesterId, requesterId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@RequestParam Long requesterId) {
        return ResponseEntity.ok(notificationService.unreadCount(requesterId, requesterId));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @RequestParam Long requesterId) {
        notificationService.markRead(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@RequestParam Long requesterId) {
        notificationService.markAllRead(requesterId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
