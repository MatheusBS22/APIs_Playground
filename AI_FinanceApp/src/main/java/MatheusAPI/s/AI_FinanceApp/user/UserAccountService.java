package MatheusAPI.s.AI_FinanceApp.user;
import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.group.GroupRepository;
import MatheusAPI.s.AI_FinanceApp.group.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;
    private final PasswordEncoder passwordEncoder;

    // Sem 0/O/1/I pra ninguém confundir na hora de digitar o código.
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Toda conta nova nasce MEMBER. Não existe mais criar conta já como Family Manager ou Developer --
    // o usuário vira Family Manager ao montar um grupo familiar (ver GroupService.createFamilyGroup).
    @Transactional
    public UserAccount create(String name, String surname, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória");
        }
        if (userAccountRepository.existsByUsername(name)) {
            throw new IllegalArgumentException("Esse nome de usuário já está em uso");
        }

        Group group = groupService.create(name + " " + "Group");
        UserAccount userAccount = new UserAccount();
        userAccount.setGroup(group);
        userAccount.setUsername(name);
        userAccount.setSurname(surname);
        userAccount.setAccType(AccType.MEMBER);
        userAccount.setAccPermissions(AccType.MEMBER.getDefaultPermissions());
        userAccount.setPassword(passwordEncoder.encode(rawPassword));
        userAccount.setInviteCode(generateUniqueInviteCode());
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

    // Só o próprio dono (ou DEVELOPER) enxerga o código de convite. Contas antigas, criadas antes
    // desse campo existir, ganham um código na primeira vez que alguém pedir.
    @Transactional
    public String getOrCreateInviteCode(Long id, Long requesterId) {
        requireSelfOrDeveloper(id, requesterId, "ver o código de convite desse usuário");
        UserAccount user = getById(id);
        if (user.getInviteCode() == null || user.getInviteCode().isBlank()) {
            user.setInviteCode(generateUniqueInviteCode());
            userAccountRepository.save(user);
        }
        return user.getInviteCode();
    }

    // Se o código vazou, o dono troca -- o antigo deixa de valer na hora.
    @Transactional
    public String regenerateInviteCode(Long id, Long requesterId) {
        requireSelfOrDeveloper(id, requesterId, "trocar o código de convite desse usuário");
        UserAccount user = getById(id);
        user.setInviteCode(generateUniqueInviteCode());
        userAccountRepository.save(user);
        return user.getInviteCode();
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (!userAccountRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código de convite único, tente de novo");
    }

    void requireSelfOrDeveloper(Long targetId, Long requesterId, String action) {
        UserAccount requester = getById(requesterId);
        if (requester.getAccType() == AccType.DEVELOPER) return;
        if (!targetId.equals(requesterId)) {
            throw new AccessDeniedException("Você não tem permissão para " + action);
        }
    }
}
