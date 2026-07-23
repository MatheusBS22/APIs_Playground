package MatheusAPI.s.AI_FinanceApp.user;

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

    @Transactional
    public UserAccount updateName(Long id, String newName) {
        UserAccount userAccount = getById(id);
        userAccount.setUsername(newName);
        return userAccountRepository.save(userAccount);
    }

    @Transactional
    public UserAccount updateSurname(Long id, String newSurname) {
        UserAccount userAccount = getById(id);
        userAccount.setSurname(newSurname);
        return userAccountRepository.save(userAccount);
    }

    @Transactional
    public void delete(Long id) {
        UserAccount userAccount = getById(id);
        userAccountRepository.delete(userAccount);
    }

}
