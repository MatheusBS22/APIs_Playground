package MatheusAPI.s.AI_FinanceApp.goal;

import MatheusAPI.s.AI_FinanceApp.balanceflow.BalanceFlow;
import MatheusAPI.s.AI_FinanceApp.balanceflow.BalanceFlowService;
import MatheusAPI.s.AI_FinanceApp.balanceflow.FlowType;
import MatheusAPI.s.AI_FinanceApp.category.Category;
import MatheusAPI.s.AI_FinanceApp.category.CategoryService;
import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.group.GroupRepository;
import MatheusAPI.s.AI_FinanceApp.notification.NotificationService;
import MatheusAPI.s.AI_FinanceApp.notification.NotificationType;
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
    private final NotificationService notificationService;
    private final BalanceFlowService balanceFlowService;
    private final CategoryService categoryService;

    // Marcos que disparam aviso quando a meta cruza esse percentual (do maior pro menor,
    // pra avisar só o marco mais alto que foi cruzado num único aporte).
    private static final int[] MILESTONES = {100, 75, 50};

    // Toda quantia em dinheiro no app usa no máximo 2 casas decimais -- o que vier com mais
    // é arredondado (compactado) pra esse limite, tanto no aporte quanto no valor guardado na meta.
    private static final int MONEY_SCALE = 2;

    // Nome fixo da categoria de sistema onde caem os aportes em metas -- uma por grupo, criada sozinha.
    private static final String CONTRIBUTION_CATEGORY_NAME = "Aportes em metas";

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
    // Em meta individual, só o dono contribui. Dispara avisos de aporte e de marco cruzado (50/75/100%).
    // O valor sai de verdade de uma carteira do próprio requester (lançado como despesa), então quem
    // aporta precisa ter uma carteira -- o dinheiro não aparece do nada.
    @Transactional
    public ContributionResult addContribution(Long id, BigDecimal amount, Long accountId, Long requesterId) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("O valor do aporte deve ser positivo");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("Informe de qual carteira o aporte vai sair");
        }
        amount = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

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

        // Lança o aporte como despesa na carteira do requester. BalanceFlowService já garante que a
        // carteira é dele (ou DEVELOPER) e que ela pertence ao mesmo grupo da meta/categoria.
        Category category = categoryService.getOrCreateSystemCategory(
                CONTRIBUTION_CATEGORY_NAME, FlowType.EXPENSE, goal.getGroup().getId());
        // título da despesa tem limite de 32 caracteres na coluna -- corta se o nome da meta for longo.
        String title = "Aporte: " + goal.getName();
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        BalanceFlow flow = balanceFlowService.create(
                FlowType.EXPENSE, amount, title, null,
                accountId, category.getId(), requesterId);

        BigDecimal before = goal.getCurrentAmount();
        BigDecimal after = before.add(amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        goal.setCurrentAmount(after);
        Goal saved = goalRepository.save(goal);

        notifyContribution(saved, requester, amount, before, after);

        // Não bloqueia se a carteira ficar negativa -- só avisa. É "número calculado" (saldo = soma
        // dos lançamentos), então negativo não quebra nada, mas o usuário precisa saber.
        BigDecimal accountBalanceAfter = balanceFlowService.getBalanceByAccount(flow.getAccount().getId(), requesterId);
        boolean lowBalanceWarning = accountBalanceAfter.signum() < 0;

        return new ContributionResult(saved, lowBalanceWarning, accountBalanceAfter);
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

    // ---------- avisos ----------

    private void notifyContribution(Goal goal, UserAccount contributor, BigDecimal amount, BigDecimal before, BigDecimal after) {
        String amountFmt = NotificationService.formatCurrency(amount);
        String contributionMessage = contributor.getUsername() + " aportou " + amountFmt + " na meta " + goal.getName();

        List<UserAccount> notifyTargets;
        if (goal.getOwner() == null) {
            // meta de grupo: avisa todo mundo do grupo, menos quem acabou de aportar
            notifyTargets = userAccountRepository.findByGroupId(goal.getGroup().getId());
            notificationService.notifyOthers(notifyTargets, contributor.getId(), NotificationType.GOAL_CONTRIBUTION, contributionMessage);
        } else {
            // meta individual: só o dono aporta, então não faz sentido avisar "fulano aportou" pra ele mesmo
            notifyTargets = List.of(goal.getOwner());
        }

        int milestone = crossedMilestone(goal.getTargetAmount(), before, after);
        if (milestone > 0) {
            String milestoneMessage = "A meta " + goal.getName() + " passou de " + milestone + "%!";
            for (UserAccount target : notifyTargets) {
                notificationService.notify(target, NotificationType.GOAL_MILESTONE, milestoneMessage);
            }
            if (goal.getOwner() == null) {
                notificationService.notify(contributor, NotificationType.GOAL_MILESTONE, milestoneMessage);
            }
        }
    }

    // Maior marco (100/75/50) que o aporte fez a meta cruzar, ou -1 se nenhum foi cruzado agora.
    private int crossedMilestone(BigDecimal target, BigDecimal before, BigDecimal after) {
        for (int milestone : MILESTONES) {
            BigDecimal threshold = target.multiply(BigDecimal.valueOf(milestone))
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            if (after.compareTo(threshold) >= 0 && before.compareTo(threshold) < 0) {
                return milestone;
            }
        }
        return -1;
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

// accountBalanceAfter: saldo da carteira depois do aporte. lowBalanceWarning: true se ficou negativo
// -- o front usa isso pra avisar o usuário, sem bloquear o aporte.
record ContributionResult(Goal goal, boolean lowBalanceWarning, BigDecimal accountBalanceAfter) {}
