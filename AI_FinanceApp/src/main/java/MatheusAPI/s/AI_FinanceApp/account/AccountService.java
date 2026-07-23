package MatheusAPI.s.AI_FinanceApp.account;

import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.group.GroupRepository;
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

    public List<Account> listByGroup(Long groupId) {
        return accountRepository.findByGroupId(groupId);
    }

    public Account getById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + id));
    }

    @Transactional
    public Account update(Long id, String newName) {
        Account account = getById(id);
        account.setName(newName);
        return accountRepository.save(account);
    }

    @Transactional
    public void delete(Long id) {
        Account account = getById(id);
        accountRepository.delete(account);
    }

}
