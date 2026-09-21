package MatheusAPI.s.AI_FinanceApp.group;

import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.notification.NotificationService;
import MatheusAPI.s.AI_FinanceApp.notification.NotificationType;
import MatheusAPI.s.AI_FinanceApp.user.AccPermissions;
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
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    // Nenhum grupo familiar pode passar disso -- por enquanto é fixo, sem plano diferenciado.
    public static final int MAX_FAMILY_GROUP_SIZE = 14;

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
    // Mantido pra uso administrativo -- o fluxo normal de entrada é via código de convite (joinByInviteCode).
    @Transactional
    public UserAccount addMember(Long groupId, Long userId, Long requesterId) {
        requireGroupManagerOf(groupId, requesterId, "adicionar membro nesse grupo");
        requireHasRoomFor(groupId, 1);
        Group group = getById(groupId);
        UserAccount member = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));
        member.setGroup(group);
        return userAccountRepository.save(member);
    }

    // Kick feito pelo manager (ou DEVELOPER). O membro removido volta pra um grupo pessoal novo.
    @Transactional
    public UserAccount removeMember(Long groupId, Long userId, Long requesterId) {
        requireGroupManagerOf(groupId, requesterId, "remover membro desse grupo");
        UserAccount member = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));

        if (!member.getGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Esse usuário não pertence a esse grupo");
        }

        sendToNewPersonalGroup(member);
        return userAccountRepository.save(member);
    }

    // ---------- fluxo novo: montar grupo, entrar por código, sair ----------

    // Verdadeiro se o usuário já está de fato num grupo familiar: ou já é o manager de um,
    // ou está num grupo (mesmo que "pessoal" de nascença) que já tem mais alguém dentro.
    public boolean isInFamilyGroup(UserAccount user) {
        if (user.getAccType() == AccType.FAMILY_MANAGER) return true;
        return userAccountRepository.countByGroupId(user.getGroup().getId()) > 1;
    }

    // Transforma o grupo pessoal do próprio usuário num grupo familiar, e ele vira o manager dele.
    @Transactional
    public Group createFamilyGroup(Long requesterId, String name) {
        UserAccount requester = getUser(requesterId);
        if (isInFamilyGroup(requester)) {
            throw new IllegalArgumentException("Você já pertence a um grupo familiar. Saia dele antes de criar outro.");
        }

        Group group = requester.getGroup();
        if (name != null && !name.isBlank()) {
            group.setName(name);
            groupRepository.save(group);
        }

        requester.setAccType(AccType.FAMILY_MANAGER);
        var permissions = requester.getAccPermissions();
        permissions.add(AccPermissions.MANAGE_FAMILY_GROUP);
        requester.setAccPermissions(permissions);
        userAccountRepository.save(requester);

        return group;
    }

    // Entra num grupo familiar usando o código de convite de quem já está lá dentro.
    @Transactional
    public UserAccount joinByInviteCode(String inviteCode, Long requesterId) {
        UserAccount requester = getUser(requesterId);
        if (isInFamilyGroup(requester)) {
            throw new IllegalArgumentException("Você já pertence a um grupo familiar. Saia dele antes de entrar em outro.");
        }

        UserAccount inviter = userAccountRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("Código de convite inválido"));
        if (inviter.getId().equals(requester.getId())) {
            throw new IllegalArgumentException("Você não pode usar o seu próprio código de convite");
        }

        Group targetGroup = inviter.getGroup();
        requireHasRoomFor(targetGroup.getId(), 1);

        List<UserAccount> existingMembers = userAccountRepository.findByGroupId(targetGroup.getId());

        requester.setGroup(targetGroup);
        UserAccount saved = userAccountRepository.save(requester);

        notificationService.notifyOthers(existingMembers, requester.getId(), NotificationType.MEMBER_JOINED,
                requester.getUsername() + " entrou no grupo");

        return saved;
    }

    // Sair por conta própria (sem precisar de outro manager). Se quem sai é o manager, o grupo
    // inteiro se desfaz: todo mundo, incluindo ele, volta a ter um grupo pessoal só seu.
    @Transactional
    public void leaveGroup(Long requesterId) {
        UserAccount requester = getUser(requesterId);
        boolean isOwner = requester.getAccType() == AccType.FAMILY_MANAGER;

        if (!isOwner) {
            Long groupId = requester.getGroup().getId();
            List<UserAccount> remainingMembers = userAccountRepository.findByGroupId(groupId);

            sendToNewPersonalGroup(requester);
            userAccountRepository.save(requester);

            notificationService.notifyOthers(remainingMembers, requester.getId(), NotificationType.MEMBER_LEFT,
                    requester.getUsername() + " saiu do grupo");
            return;
        }

        // O manager saindo desfaz o grupo inteiro -- não faz sentido avisar ninguém,
        // já que todo mundo (inclusive quem saiu) está voltando a não ter grupo.
        Long groupId = requester.getGroup().getId();
        List<UserAccount> members = userAccountRepository.findByGroupId(groupId);
        for (UserAccount member : members) {
            sendToNewPersonalGroup(member);
            if (member.getId().equals(requester.getId())) {
                member.setAccType(AccType.MEMBER);
                member.setAccPermissions(AccType.MEMBER.getDefaultPermissions());
            }
            userAccountRepository.save(member);
        }
    }

    // ---------- helpers ----------

    private void sendToNewPersonalGroup(UserAccount member) {
        Group personalGroup = create(member.getUsername() + " Group");
        member.setGroup(personalGroup);
    }

    private void requireHasRoomFor(Long groupId, int extra) {
        long current = userAccountRepository.countByGroupId(groupId);
        if (current + extra > MAX_FAMILY_GROUP_SIZE) {
            throw new IllegalArgumentException("Esse grupo já atingiu o limite de " + MAX_FAMILY_GROUP_SIZE + " membros");
        }
    }

    private UserAccount getUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));
    }

    private void requireGroupManagerOf(Long groupId, Long requesterId, String action) {
        UserAccount requester = getUser(requesterId);

        if (requester.getAccType() == AccType.DEVELOPER) return;

        boolean hasPermission = requester.getAccPermissions().contains(AccPermissions.MANAGE_FAMILY_GROUP);
        boolean sameGroup = requester.getGroup().getId().equals(groupId);

        if (!hasPermission || !sameGroup) {
            throw new AccessDeniedException("Você não tem permissão para " + action);
        }
    }
}
