package MatheusAPI.s.AI_FinanceApp.goal;

import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.group.GroupRepository;
import MatheusAPI.s.AI_FinanceApp.user.AccPermissions;
import MatheusAPI.s.AI_FinanceApp.user.AccType;
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
    public Goal create(String name, BigDecimal targetAmount, LocalDate deadline, Long groupId, Long ownerId, Long requesterId) {
        if (targetAmount == null || targetAmount.signum() <= 0) {
            throw new IllegalArgumentException("O valor da meta deve ser positivo");
        }
        if (deadline == null || !deadline.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("O prazo precisa ser uma data futura");
        }

        UserAccount requester = getRequester(requesterId);

        if (ownerId == null) {
            // meta de grupo -- recurso do grupo, só quem administra cria
            requireGroupManagerOrDeveloper(requester, "criar uma meta de grupo");
        } else if (!ownerId.equals(requesterId) && requester.getAccType() != AccType.DEVELOPER) {
            // meta individual só pode ser criada pelo próprio dono
            throw new AccessDeniedException("Você só pode criar meta individual em seu próprio nome");
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

    // Ver metas de grupo é liberado pra qualquer membro -- só visualização.
    public List<Goal> listGroupGoals(Long groupId) {
        return goalRepository.findByGroupIdAndOwnerIsNull(groupId);
    }

    // Meta individual só o próprio dono vê (ou DEVELOPER).
    public List<Goal> listByOwner(Long ownerId, Long requesterId) {
        UserAccount requester = getRequester(requesterId);
        if (requester.getAccType() != AccType.DEVELOPER && !ownerId.equals(requesterId)) {
            throw new AccessDeniedException("Você não pode ver metas individuais de outra pessoa");
        }
        return goalRepository.findByOwnerId(ownerId);
    }

    // Aporte: em meta de grupo, qualquer membro do mesmo grupo pode contribuir.
    // Em meta individual, só o dono contribui.
    @Transactional
    public Goal addContribution(Long id, BigDecimal amount, Long requesterId) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("O valor do aporte deve ser positivo");
        }
        Goal goal = getById(id);
        UserAccount requester = getRequester(requesterId);

        boolean isDeveloper = requester.getAccType() == AccType.DEVELOPER;
        if (goal.getOwner() == null) {
            boolean sameGroup = requester.getGroup().getId().equals(goal.getGroup().getId());
            if (!isDeveloper && !sameGroup) {
                throw new AccessDeniedException("Você só pode contribuir em metas do seu próprio grupo");
            }
        } else if (!isDeveloper && !goal.getOwner().getId().equals(requesterId)) {
            throw new AccessDeniedException("Essa meta é individual -- só o dono pode aportar");
        }

        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        return goalRepository.save(goal);
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        Goal goal = getById(id);
        UserAccount requester = getRequester(requesterId);

        if (goal.getOwner() == null) {
            requireGroupManagerOrDeveloper(requester, "apagar uma meta de grupo");
        } else if (requester.getAccType() != AccType.DEVELOPER && !goal.getOwner().getId().equals(requesterId)) {
            throw new AccessDeniedException("Você não pode apagar a meta individual de outra pessoa");
        }

        goalRepository.delete(goal);
    }

    // ---------- cálculos (sem restrição de posse -- são só leitura derivada) ----------

    private long monthsUntil(LocalDate deadline) {
        return Math.max(1, ChronoUnit.MONTHS.between(LocalDate.now(), deadline));
    }

    public BigDecimal getRequiredMonthlyContribution(Long id) {
        Goal goal = getById(id);
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        if (remaining.signum() <= 0) return BigDecimal.ZERO;
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

        long monthsNeeded = remaining.divide(proposedMonthly, 0, RoundingMode.UP).longValue();
        LocalDate projectedDate = LocalDate.now().plusMonths(monthsNeeded);
        long monthsToDeadline = monthsUntil(goal.getDeadline());
        boolean onTime = monthsNeeded <= monthsToDeadline;

        BigDecimal requiredMonthly = getRequiredMonthlyContribution(id);
        BigDecimal monthlyDifference = proposedMonthly.subtract(requiredMonthly);

        return new GoalProjection(monthsNeeded, projectedDate, onTime, monthlyDifference);
    }

    // ---------- helpers ----------

    private UserAccount getRequester(Long requesterId) {
        return userAccountRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + requesterId));
    }

    private void requireGroupManagerOrDeveloper(UserAccount requester, String action) {
        boolean allowed = requester.getAccType() == AccType.DEVELOPER
                || requester.getAccPermissions().contains(AccPermissions.MANAGE_FAMILY_GROUP);
        if (!allowed) {
            throw new AccessDeniedException("Você não tem permissão para " + action + " -- só o Family Manager pode.");
        }
    }
}

record GoalProjection(
        long monthsNeeded,
        LocalDate projectedCompletionDate,
        boolean onTrack,
        BigDecimal monthlyDifference
) {}
