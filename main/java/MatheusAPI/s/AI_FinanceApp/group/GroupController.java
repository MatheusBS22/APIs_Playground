package MatheusAPI.s.AI_FinanceApp.group;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

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
}

record CreateGroupRequest(String name) {}
record UpdateGroupNameRequest(String name, Long requesterId) {}
