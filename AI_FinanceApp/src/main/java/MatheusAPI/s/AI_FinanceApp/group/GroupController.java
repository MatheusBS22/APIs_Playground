package MatheusAPI.s.AI_FinanceApp.group;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import MatheusAPI.s.AI_FinanceApp.user.UserAccountService;
import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final UserAccountService userAccountService;

    @PostMapping
    public ResponseEntity<Group> create(@RequestBody CreateGroupRequest request) {
        Group group = groupService.create(request.name());
        return ResponseEntity.ok(group);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Group> getById(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Group> update(@PathVariable Long id, @RequestBody UpdateGroupNameRequest request) {
        return ResponseEntity.ok(groupService.update(id, request.name(), request.requesterId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long requesterId) {
        groupService.delete(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<UserAccount> addMember(@PathVariable Long groupId, @PathVariable Long userId, @RequestParam Long requesterId) {
        return ResponseEntity.ok(groupService.addMember(groupId, userId, requesterId));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<UserAccount> removeMember(@PathVariable Long groupId, @PathVariable Long userId, @RequestParam Long requesterId) {
        return ResponseEntity.ok(groupService.removeMember(groupId, userId, requesterId));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<UserAccount>> listMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(userAccountService.listByGroup(groupId));
    }

    // ---------- fluxo novo ----------

    // Monta um grupo familiar a partir do grupo pessoal do próprio usuário; ele vira o manager.
    @PostMapping("/family")
    public ResponseEntity<Group> createFamilyGroup(@RequestParam Long requesterId, @RequestBody(required = false) CreateGroupRequest request) {
        String name = request != null ? request.name() : null;
        return ResponseEntity.ok(groupService.createFamilyGroup(requesterId, name));
    }

    // Entra num grupo familiar usando o código de convite de quem já está lá dentro.
    @PostMapping("/join")
    public ResponseEntity<UserAccount> join(@RequestParam String inviteCode, @RequestParam Long requesterId) {
        return ResponseEntity.ok(groupService.joinByInviteCode(inviteCode, requesterId));
    }

    // Sai por conta própria. Se quem sai é o manager, o grupo inteiro se desfaz -- o front precisa
    // avisar isso antes de chamar esse endpoint.
    @PostMapping("/leave")
    public ResponseEntity<Void> leave(@RequestParam Long requesterId) {
        groupService.leaveGroup(requesterId);
        return ResponseEntity.noContent().build();
    }
}

record CreateGroupRequest(String name) {}
record UpdateGroupNameRequest(String name, Long requesterId) {}
