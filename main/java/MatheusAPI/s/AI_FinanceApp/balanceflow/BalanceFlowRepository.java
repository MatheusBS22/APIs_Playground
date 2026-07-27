package MatheusAPI.s.AI_FinanceApp.balanceflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BalanceFlowRepository extends JpaRepository<BalanceFlow, Long> {
    List<BalanceFlow> findByGroupId(Long groupId);
    List<BalanceFlow> findByAccountId(Long accountId);
}
