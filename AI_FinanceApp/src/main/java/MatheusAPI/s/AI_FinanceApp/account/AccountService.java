package MatheusAPI.s.AI_FinanceApp.account;

import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.group.GroupRepository;
import MatheusAPI.s.AI_FinanceApp.user.AccType;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import MatheusAPI.s.AI_FinanceApp.user.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final GroupRepository groupRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public Account create(String name, Long groupId, Long creatorId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado: " + groupId));

        UserAccount creator = userAccountRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + creatorId));

        Account account = new Account();
        account.setName(name);
        account.setGroup(group);
        account.setCreator(creator);
        account.setCreatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    // Carteira é sempre privada: só quem tem acesso irrestrito (DEVELOPER) enxerga a de outra pessoa.
    // Nem FAMILY_MANAGER tem passe livre aqui -- combinado explicitamente.
    public List<Account> listByGroup(Long groupId, Long requesterId) {
        UserAccount requester = getRequester(requesterId);
        if (requester.getAccType() == AccType.DEVELOPER) {
            return accountRepository.findByGroupId(groupId);
        }
        return accountRepository.findByGroupId(groupId).stream()
                .filter(a -> a.getCreator().getId().equals(requesterId))
                .toList();
    }

    public Account getById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + id));
    }

    @Transactional
    public Account update(Long id, String newName, Long requesterId) {
        Account account = getById(id);
        requireOwnerOrDeveloper(account.getCreator().getId(), requesterId, "editar essa carteira");
        account.setName(newName);
        return accountRepository.save(account);
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        Account account = getById(id);
        requireOwnerOrDeveloper(account.getCreator().getId(), requesterId, "apagar essa carteira");
        accountRepository.delete(account);
    }

    // ---------- helpers de posse/permissão ----------

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
