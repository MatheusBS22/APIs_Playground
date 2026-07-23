package MatheusAPI.s.AI_FinanceApp.balanceflow;

import MatheusAPI.s.AI_FinanceApp.account.Account;
import MatheusAPI.s.AI_FinanceApp.account.AccountRepository;
import MatheusAPI.s.AI_FinanceApp.category.Category;
import MatheusAPI.s.AI_FinanceApp.category.CategoryRepository;
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

    public List<BalanceFlow> listByGroup(Long groupId) {
        return balanceFlowRepository.findByGroupId(groupId);
    }

    public List<BalanceFlow> listByAccount(Long accountId) {
        return balanceFlowRepository.findByAccountId(accountId);
    }

    public BigDecimal getBalanceByAccount(Long accountId) {
        return listByAccount(accountId).stream()
                .map(flow -> flow.getType() == FlowType.INCOME ? flow.getAmount() : flow.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getBalanceByGroup(Long groupId) {
        return listByGroup(groupId).stream()
                .map(flow -> flow.getType() == FlowType.INCOME ? flow.getAmount() : flow.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<Long, BigDecimal> getCategoryBreakdown(Long groupId) {
        return listByGroup(groupId).stream()
                .collect(Collectors.groupingBy(
                        flow -> flow.getCategory().getId(),
                        Collectors.reducing(BigDecimal.ZERO, BalanceFlow::getAmount, BigDecimal::add)
                ));
    }

    public BigDecimal getTotalByCategory(Long groupId, Long categoryId) {
        return listByGroup(groupId).stream()
                .filter(flow -> flow.getCategory().getId().equals(categoryId))
                .map(BalanceFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalByPeriod(Long groupId, FlowType type, LocalDateTime start, LocalDateTime end) {
        return listByGroup(groupId).stream()
                .filter(flow -> flow.getType() == type)
                .filter(flow -> !flow.getCreateTime().isBefore(start) && !flow.getCreateTime().isAfter(end))
                .map(BalanceFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public BalanceFlow update(Long id, BigDecimal newAmount, String newTitle, String newNotes) {
        BalanceFlow flow = getById(id);

        if (newAmount == null || newAmount.signum() <= 0) {
            throw new IllegalArgumentException("O valor do lançamento deve ser positivo");
        }

        flow.setAmount(newAmount);
        flow.setTitle(newTitle);
        flow.setNotes(newNotes);
        return balanceFlowRepository.save(flow);
    }

    @Transactional
    public void delete(Long id) {
        BalanceFlow flow = getById(id);
        balanceFlowRepository.delete(flow);
    }

}
