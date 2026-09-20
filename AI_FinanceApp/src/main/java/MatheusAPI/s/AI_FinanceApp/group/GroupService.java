package MatheusAPI.s.AI_FinanceApp.group;

import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.user.AccPermissions;
import MatheusAPI.s.AI_FinanceApp.user.AccType;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import MatheusAPI.s.AI_FinanceApp.user.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserAccountRepository userAccountRepository;

    // Usado internamente pelo UserAccountService ao criar o grupo pessoal de um usuário novo -- sem checagem,
    // porque nesse momento o usuário ainda não existe pra ter um requesterId.
    @Transactional
    public Group create(String name) {
        Group group = new Group();
        group.setName(name);
        group.setCreatedAt(LocalDateTime.now());
        return groupRepository.save(group);
    }

    public Group getById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado: " + id));
    }

    @Transactional
    public Group update(Long id, String newName, Long requesterId) {
        requireGroupManagerOf(id, requesterId, "renomear esse grupo");
        Group group = getById(id);
        group.setName(newName);
        return groupRepository.save(group);
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        requireGroupManagerOf(id, requesterId, "apagar esse grupo");
        Group group = getById(id);
        groupRepository.delete(group);
    }

        // Move um usuário existente pra dentro deste grupo. Só quem gerencia o grupo (ou DEVELOPER) pode chamar.
    @Transactional
    public UserAccount addMember(Long groupId, Long userId, Long requesterId) {
        requireGroupManagerOf(groupId, requesterId, "adicionar membro nesse grupo");
        Group group = getById(groupId);
        UserAccount member = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));
        member.setGroup(group);
        return userAccountRepository.save(member);
    }

    // Tira o usuário do grupo, devolvendo ele pro próprio grupo pessoal (nunca fica sem grupo).
    @Transactional
    public UserAccount removeMember(Long groupId, Long userId, Long requesterId) {
        requireGroupManagerOf(groupId, requesterId, "remover membro desse grupo");
        UserAccount member = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));

        if (!member.getGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Esse usuário não pertence a esse grupo");
        }

        Group personalGroup = create(member.getUsername() + " Group");
        member.setGroup(personalGroup);
        return userAccountRepository.save(member);
    }

    private void requireGroupManagerOf(Long groupId, Long requesterId, String action) {
        UserAccount requester = userAccountRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + requesterId));

        if (requester.getAccType() == AccType.DEVELOPER) return;

        boolean hasPermission = requester.getAccPermissions().contains(AccPermissions.MANAGE_FAMILY_GROUP);
        boolean sameGroup = requester.getGroup().getId().equals(groupId);

        if (!hasPermission || !sameGroup) {
            throw new AccessDeniedException("Você não tem permissão para " + action);
        }
    }
}
