package MatheusAPI.s.AI_FinanceApp.goal;

import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.group.GroupRepository;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import MatheusAPI.s.AI_FinanceApp.user.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final GroupRepository groupRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public Goal create(String name, BigDecimal targetAmount, LocalDate deadline, Long groupId, Long ownerId) {
        if (targetAmount == null || targetAmount.signum() <= 0) {
            throw new IllegalArgumentException("O valor da meta deve ser positivo");
        }
        if (deadline == null || !deadline.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("O prazo precisa ser uma data futura");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado: " + groupId));

        UserAccount owner = null;
        if (ownerId != null) {
            owner = userAccountRepository.findById(ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + ownerId));
        }

        Goal goal = new Goal();
        goal.setName(name);
        goal.setTargetAmount(targetAmount);
        goal.setDeadline(deadline);
        goal.setGroup(group);
        goal.setOwner(owner);

        return goalRepository.save(goal);
    }

    public Goal getById(Long id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada: " + id));
    }

    public List<Goal> listGroupGoals(Long groupId) {
        return goalRepository.findByGroupIdAndOwnerIsNull(groupId);
    }

    public List<Goal> listByOwner(Long ownerId) {
        return goalRepository.findByOwnerId(ownerId);
    }

    @Transactional
    public Goal addContribution(Long id, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("O valor do aporte deve ser positivo");
        }
        Goal goal = getById(id);
        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        return goalRepository.save(goal);
    }

    @Transactional
    public void delete(Long id) {
        Goal goal = getById(id);
        goalRepository.delete(goal);
    }

    // ---------- cálculos ----------

    private long monthsUntil(LocalDate deadline) {
        return Math.max(1, ChronoUnit.MONTHS.between(LocalDate.now(), deadline));
    }

    public BigDecimal getRequiredMonthlyContribution(Long id) {
        Goal goal = getById(id);
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());

        if (remaining.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        long months = monthsUntil(goal.getDeadline());
        return remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    public GoalProjection simulateMonthlyContribution(Long id, BigDecimal proposedMonthly) {
        if (proposedMonthly == null || proposedMonthly.signum() <= 0) {
            throw new IllegalArgumentException("O valor mensal proposto deve ser positivo");
        }

        Goal goal = getById(id);
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());

        if (remaining.signum() <= 0) {
            return new GoalProjection(0, LocalDate.now(), true, proposedMonthly);
        }

        long monthsNeeded = remaining
                .divide(proposedMonthly, 0, RoundingMode.UP)
                .longValue();

        LocalDate projectedDate = LocalDate.now().plusMonths(monthsNeeded);
        long monthsToDeadline = monthsUntil(goal.getDeadline());
        boolean onTime = monthsNeeded <= monthsToDeadline;

        BigDecimal requiredMonthly = getRequiredMonthlyContribution(id);
        BigDecimal monthlyDifference = proposedMonthly.subtract(requiredMonthly);

        return new GoalProjection(monthsNeeded, projectedDate, onTime, monthlyDifference);
    }
}

record GoalProjection(
        long monthsNeeded,
        LocalDate projectedCompletionDate,
        boolean onTrack,
        BigDecimal monthlyDifference // positivo = sobra por mês, negativo = falta por mês
) {}
