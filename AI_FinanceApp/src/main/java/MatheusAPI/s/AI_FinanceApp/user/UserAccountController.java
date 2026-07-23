package MatheusAPI.s.AI_FinanceApp.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping
    public ResponseEntity<UserAccount> create(@RequestBody CreateUserAccountRequest request) {
        UserAccount userAccount = userAccountService.create(request.accType(),
                request.username(),
                request.surname());
        return ResponseEntity.ok(userAccount);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccount> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userAccountService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccount> update(@PathVariable Long id, @RequestBody UpdateUserAccountNameRequest request) {
        return ResponseEntity.ok(userAccountService.updateName(id, request.name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userAccountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

record CreateUserAccountRequest(AccType accType, String username, String surname) {}
record UpdateUserAccountNameRequest(String name) {}
