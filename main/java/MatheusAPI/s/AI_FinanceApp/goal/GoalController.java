package MatheusAPI.s.AI_FinanceApp.goal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<Goal> create(@RequestBody CreateGoalRequest request) {
        Goal goal = goalService.create(
                request.name(), request.targetAmount(), request.deadline(),
                request.groupId(), request.ownerId(), request.requesterId());
        return ResponseEntity.ok(goal);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Goal> getById(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getById(id));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Goal>> listGroupGoals(@PathVariable Long groupId) {
        return ResponseEntity.ok(goalService.listGroupGoals(groupId));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Goal>> listByOwner(@PathVariable Long ownerId, @RequestParam Long requesterId) {
        return ResponseEntity.ok(goalService.listByOwner(ownerId, requesterId));
    }

    @PostMapping("/{id}/contributions")
    public ResponseEntity<Goal> addContribution(@PathVariable Long id, @RequestBody ContributionRequest request) {
        return ResponseEntity.ok(goalService.addContribution(id, request.amount(), request.requesterId()));
    }

    @GetMapping("/{id}/required-monthly")
    public ResponseEntity<BigDecimal> getRequiredMonthlyContribution(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getRequiredMonthlyContribution(id));
    }

    @GetMapping("/{id}/simulate")
    public ResponseEntity<GoalProjection> simulateMonthlyContribution(@PathVariable Long id, @RequestParam BigDecimal monthlyAmount) {
        return ResponseEntity.ok(goalService.simulateMonthlyContribution(id, monthlyAmount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long requesterId) {
        goalService.delete(id, requesterId);
        return ResponseEntity.noContent().build();
    }
}

record CreateGoalRequest(String name, BigDecimal targetAmount, LocalDate deadline, Long groupId, Long ownerId, Long requesterId) {}
record ContributionRequest(BigDecimal amount, Long requesterId) {}
