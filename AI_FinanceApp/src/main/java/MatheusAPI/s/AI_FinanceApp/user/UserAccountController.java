package MatheusAPI.s.AI_FinanceApp.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    // Cadastro não recebe mais accType: toda conta nova nasce MEMBER.
    // Vira Family Manager ao montar um grupo familiar (POST /groups/family).
    @PostMapping
    public ResponseEntity<UserAccount> create(@RequestBody CreateUserAccountRequest request) {
        UserAccount userAccount = userAccountService.create(request.username(), request.surname(), request.password());
        return ResponseEntity.ok(userAccount);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccount> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userAccountService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccount> update(@PathVariable Long id, @RequestBody UpdateUserAccountNameRequest request) {
        return ResponseEntity.ok(userAccountService.updateName(id, request.name(), request.requesterId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long requesterId) {
        userAccountService.delete(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    // Só o próprio dono (ou DEVELOPER) consegue ver isso -- o requesterId precisa bater com o id da URL.
    @GetMapping("/{id}/invite-code")
    public ResponseEntity<Map<String, String>> getInviteCode(@PathVariable Long id, @RequestParam Long requesterId) {
        return ResponseEntity.ok(Map.of("inviteCode", userAccountService.getOrCreateInviteCode(id, requesterId)));
    }

    @PostMapping("/{id}/invite-code/regenerate")
    public ResponseEntity<Map<String, String>> regenerateInviteCode(@PathVariable Long id, @RequestParam Long requesterId) {
        return ResponseEntity.ok(Map.of("inviteCode", userAccountService.regenerateInviteCode(id, requesterId)));
    }
}

record CreateUserAccountRequest(String username, String surname, String password) {}
record UpdateUserAccountNameRequest(String name, Long requesterId) {}
