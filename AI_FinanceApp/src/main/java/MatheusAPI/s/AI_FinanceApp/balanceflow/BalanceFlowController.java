package MatheusAPI.s.AI_FinanceApp.balanceflow;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/balance-flows")
@RequiredArgsConstructor
public class BalanceFlowController {

    private final BalanceFlowService balanceFlowService;

    @PostMapping
    public ResponseEntity<BalanceFlow> create(@RequestBody CreateBalanceFlowRequest request) {
        BalanceFlow balanceFlow = balanceFlowService.create(
                request.type(),
                request.amount(),
                request.title(),
                request.notes(),
                request.accountId(),
                request.categoryId(),
                request.creatorId());
        return ResponseEntity.ok(balanceFlow);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BalanceFlow> getById(@PathVariable Long id) {
        return ResponseEntity.ok(balanceFlowService.getById(id));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<BalanceFlow>> listByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(balanceFlowService.listByAccount(accountId));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<BalanceFlow>> listByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(balanceFlowService.listByGroup(groupId));
    }

    @GetMapping("/account/{accountId}/balance")
    public ResponseEntity<BigDecimal> getBalanceByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(balanceFlowService.getBalanceByAccount(accountId));
    }

    @GetMapping("/group/{groupId}/balance")
    public ResponseEntity<BigDecimal> getBalanceByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(balanceFlowService.getBalanceByGroup(groupId));
    }

    @GetMapping("/group/{groupId}/breakdown")
    public ResponseEntity<Map<Long, BigDecimal>> getCategoryBreakdown(@PathVariable Long groupId) {
        return ResponseEntity.ok(balanceFlowService.getCategoryBreakdown(groupId));
    }

    @GetMapping("/group/{groupId}/category/{categoryId}/total")
    public ResponseEntity<BigDecimal> getTotalByCategory(@PathVariable Long groupId, @PathVariable Long categoryId) {
        return ResponseEntity.ok(balanceFlowService.getTotalByCategory(groupId, categoryId));
    }

    @GetMapping("/group/{groupId}/period")
    public ResponseEntity<BigDecimal> getTotalByPeriod(@PathVariable Long groupId,
                                                        @RequestParam FlowType type,
                                                        @RequestParam LocalDateTime start,
                                                        @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(balanceFlowService.getTotalByPeriod(groupId, type, start, end));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BalanceFlow> update(@PathVariable Long id, @RequestBody UpdateBalanceFlowRequest request) {
        return ResponseEntity.ok(balanceFlowService.update(id, request.newAmount(), request.newTitle(), request.newNotes()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        balanceFlowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

record CreateBalanceFlowRequest(FlowType type, BigDecimal amount, String title, String notes, Long accountId, Long categoryId, Long creatorId) {}
record UpdateBalanceFlowRequest(BigDecimal newAmount, String newTitle, String newNotes) {}
