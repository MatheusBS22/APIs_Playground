package MatheusAPI.s.AI_FinanceApp.user;

import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.group.GroupRepository;
import MatheusAPI.s.AI_FinanceApp.group.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;

    // Cadastro em si não exige requesterId -- é o próprio ato de "nascer" no sistema.
    @Transactional
    public UserAccount create(AccType accType, String name, String surname) {
        Group group = groupService.create(name + " " + "Group");
        UserAccount userAccount = new UserAccount();
        userAccount.setGroup(group);
        userAccount.setUsername(name);
        userAccount.setSurname(surname);
        userAccount.setAccType(accType);
        userAccount.setAccPermissions(accType.getDefaultPermissions());

        return userAccountRepository.save(userAccount);
    }

    public List<UserAccount> listByGroup(Long groupId) {
        return userAccountRepository.findByGroupId(groupId);
    }

    public UserAccount getById(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));
    }

    // Cada um só edita o próprio perfil -- nem Family Manager mexe no perfil de outro membro.
    @Transactional
    public UserAccount updateName(Long id, String newName, Long requesterId) {
        requireSelfOrDeveloper(id, requesterId, "editar esse usuário");
        UserAccount userAccount = getById(id);
        userAccount.setUsername(newName);
        return userAccountRepository.save(userAccount);
    }

    @Transactional
    public UserAccount updateSurname(Long id, String newSurname, Long requesterId) {
        requireSelfOrDeveloper(id, requesterId, "editar esse usuário");
        UserAccount userAccount = getById(id);
        userAccount.setSurname(newSurname);
        return userAccountRepository.save(userAccount);
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        requireSelfOrDeveloper(id, requesterId, "apagar esse usuário");
        UserAccount userAccount = getById(id);
        userAccountRepository.delete(userAccount);
    }

    private void requireSelfOrDeveloper(Long targetId, Long requesterId, String action) {
        UserAccount requester = getById(requesterId);
        if (requester.getAccType() == AccType.DEVELOPER) return;
        if (!targetId.equals(requesterId)) {
            throw new AccessDeniedException("Você não tem permissão para " + action);
        }
    }
}
