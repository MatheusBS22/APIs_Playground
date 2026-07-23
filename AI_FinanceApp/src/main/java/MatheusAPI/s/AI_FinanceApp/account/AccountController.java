package MatheusAPI.s.AI_FinanceApp.account;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<Account> create(@RequestBody CreateAccountRequest request) {
        Account account = accountService.create(request.name(), request.groupId(), request.creatorId());
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getById(id));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Account>> listByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(accountService.listByGroup(groupId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> update(@PathVariable Long id, @RequestBody UpdateAccountNameRequest request) {
        return ResponseEntity.ok(accountService.update(id, request.name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

record CreateAccountRequest(String name, Long creatorId, Long groupId) {}
record UpdateAccountNameRequest(String name) {}
