package MatheusAPI.s.AI_FinanceApp.balanceflow;

import MatheusAPI.s.AI_FinanceApp.account.Account;
import MatheusAPI.s.AI_FinanceApp.account.AccountRepository;
import MatheusAPI.s.AI_FinanceApp.category.Category;
import MatheusAPI.s.AI_FinanceApp.category.CategoryRepository;
import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.user.AccType;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import MatheusAPI.s.AI_FinanceApp.user.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceFlowService {

    private final BalanceFlowRepository balanceFlowRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public BalanceFlow create(FlowType type, BigDecimal amount, String title, String notes,
                               Long accountId, Long categoryId, Long creatorId) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("O valor do lançamento deve ser positivo");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + accountId));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + categoryId));

        UserAccount creator = userAccountRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + creatorId));

        // Carteira é privada -- só o dono lança nela (ou DEVELOPER).
        if (creator.getAccType() != AccType.DEVELOPER && !account.getCreator().getId().equals(creatorId)) {
            throw new AccessDeniedException("Você só pode lançar em carteiras que são suas");
        }

        Long groupId = account.getGroup().getId();

        if (!category.getGroup().getId().equals(groupId) || !creator.getGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Conta, categoria e usuário precisam pertencer ao mesmo grupo");
        }

        if (category.getType() != type) {
            throw new IllegalArgumentException("O tipo do lançamento não bate com o tipo da categoria");
        }

        BalanceFlow flow = new BalanceFlow();
        flow.setType(type);
        flow.setAmount(amount);
        flow.setTitle(title);
        flow.setNotes(notes);
        flow.setAccount(account);
        flow.setCategory(category);
        flow.setCreator(creator);
        flow.setGroup(account.getGroup());
        flow.setCreateTime(LocalDateTime.now());

        return balanceFlowRepository.save(flow);
    }

    public BalanceFlow getById(Long id) {
        return balanceFlowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado: " + id));
    }

    // Lista do grupo -- cada um só vê os próprios lançamentos, exceto DEVELOPER.
    public List<BalanceFlow> listByGroup(Long groupId, Long requesterId) {
        UserAccount requester = getRequester(requesterId);
        List<BalanceFlow> all = balanceFlowRepository.findByGroupId(groupId);
        if (requester.getAccType() == AccType.DEVELOPER) return all;
        return all.stream().filter(f -> f.getCreator().getId().equals(requesterId)).toList();
    }

    public List<BalanceFlow> listByAccount(Long accountId, Long requesterId) {
        UserAccount requester = getRequester(requesterId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + accountId));
        if (requester.getAccType() != AccType.DEVELOPER && !account.getCreator().getId().equals(requesterId)) {
            throw new AccessDeniedException("Você não pode ver lançamentos de uma carteira que não é sua");
        }
        return balanceFlowRepository.findByAccountId(accountId);
    }

    public BigDecimal getBalanceByAccount(Long accountId, Long requesterId) {
        return listByAccount(accountId, requesterId).stream()
                .map(flow -> flow.getType() == FlowType.INCOME ? flow.getAmount() : flow.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getBalanceByGroup(Long groupId, Long requesterId) {
        return listByGroup(groupId, requesterId).stream()
                .map(flow -> flow.getType() == FlowType.INCOME ? flow.getAmount() : flow.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<Long, BigDecimal> getCategoryBreakdown(Long groupId, Long requesterId) {
        return listByGroup(groupId, requesterId).stream()
                .collect(Collectors.groupingBy(
                        flow -> flow.getCategory().getId(),
                        Collectors.reducing(BigDecimal.ZERO, BalanceFlow::getAmount, BigDecimal::add)
                ));
    }

    public BigDecimal getTotalByCategory(Long groupId, Long categoryId, Long requesterId) {
        return listByGroup(groupId, requesterId).stream()
                .filter(flow -> flow.getCategory().getId().equals(categoryId))
                .map(BalanceFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalByPeriod(Long groupId, FlowType type, LocalDateTime start, LocalDateTime end, Long requesterId) {
        return listByGroup(groupId, requesterId).stream()
                .filter(flow -> flow.getType() == type)
                .filter(flow -> !flow.getCreateTime().isBefore(start) && !flow.getCreateTime().isAfter(end))
                .map(BalanceFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public BalanceFlow update(Long id, BigDecimal newAmount, String newTitle, String newNotes, Long requesterId) {
        BalanceFlow flow = getById(id);
        requireOwnerOrDeveloper(flow.getCreator().getId(), requesterId, "editar esse lançamento");

        if (newAmount == null || newAmount.signum() <= 0) {
            throw new IllegalArgumentException("O valor do lançamento deve ser positivo");
        }

        flow.setAmount(newAmount);
        flow.setTitle(newTitle);
        flow.setNotes(newNotes);
        return balanceFlowRepository.save(flow);
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        BalanceFlow flow = getById(id);
        requireOwnerOrDeveloper(flow.getCreator().getId(), requesterId, "apagar esse lançamento");
        balanceFlowRepository.delete(flow);
    }

    // ---------- helpers ----------

    private UserAccount getRequester(Long requesterId) {
        return userAccountRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + requesterId));
    }

    private void requireOwnerOrDeveloper(Long ownerId, Long requesterId, String action) {
        UserAccount requester = getRequester(requesterId);
        if (requester.getAccType() == AccType.DEVELOPER) return;
        if (!ownerId.equals(requesterId)) {
            throw new AccessDeniedException("Você não tem permissão para " + action);
        }
    }
}
