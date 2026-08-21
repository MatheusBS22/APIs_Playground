package MatheusAPI.s.AI_FinanceApp.category;

import MatheusAPI.s.AI_FinanceApp.balanceflow.FlowType;
import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.group.GroupRepository;
import MatheusAPI.s.AI_FinanceApp.user.AccPermissions;
import MatheusAPI.s.AI_FinanceApp.user.AccType;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import MatheusAPI.s.AI_FinanceApp.user.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final GroupRepository groupRepository;
    private final CategoryRepository categoryRepository;
    private final UserAccountRepository userAccountRepository;

    // ownerId == null  -> categoria do grupo (exige Family Manager/Developer).
    // ownerId != null  -> categoria pessoal (qualquer usuário cria a própria).
    @Transactional
    public Category create(String name, FlowType type, Long groupId, Long ownerId, Long requesterId) {
        UserAccount requester = getRequester(requesterId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado: " + groupId));

        UserAccount owner = null;

        if (ownerId != null) {
            // Categoria pessoal: cada usuário só pode criar/gerenciar a sua própria.
            if (requester.getAccType() != AccType.DEVELOPER && !ownerId.equals(requesterId)) {
                throw new AccessDeniedException("Você só pode criar categorias pessoais para você mesmo.");
            }
            owner = userAccountRepository.findById(ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + ownerId));
            if (requester.getAccType() != AccType.DEVELOPER && !owner.getGroup().getId().equals(groupId)) {
                throw new IllegalArgumentException("O dono da categoria precisa pertencer ao grupo informado");
            }
        } else {
            // Categoria de grupo: recurso do grupo -- só quem administra (ou DEVELOPER) mexe.
            requireGroupManager(requester, "criar uma categoria de grupo");
            if (requester.getAccType() != AccType.DEVELOPER && !requester.getGroup().getId().equals(groupId)) {
                throw new AccessDeniedException("Você só pode criar categorias no seu próprio grupo.");
            }
        }

        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setGroup(group);
        category.setOwner(owner);

        return categoryRepository.save(category);
    }

    // Cada usuário vê as categorias do grupo + só as próprias categorias pessoais.
    // DEVELOPER vê tudo (inclusive categorias pessoais de outros usuários).
    public List<Category> listByGroup(Long groupId, Long requesterId) {
        UserAccount requester = getRequester(requesterId);

        if (requester.getAccType() == AccType.DEVELOPER) {
            return categoryRepository.findByGroupId(groupId);
        }

        List<Category> visible = new ArrayList<>(categoryRepository.findByGroupIdAndOwnerIsNull(groupId));
        visible.addAll(categoryRepository.findByGroupIdAndOwnerId(groupId, requesterId));
        return visible;
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + id));
    }

    @Transactional
    public Category update(Long id, String newName, Long requesterId) {
        Category category = getById(id);
        requireManageCategory(category, requesterId, "editar essa categoria");
        category.setName(newName);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        Category category = getById(id);
        requireManageCategory(category, requesterId, "apagar essa categoria");
        categoryRepository.delete(category);
    }

    // ---------- helpers ----------

    private UserAccount getRequester(Long requesterId) {
        return userAccountRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + requesterId));
    }

    private void requireGroupManager(UserAccount requester, String action) {
        boolean allowed = requester.getAccType() == AccType.DEVELOPER
                || requester.getAccPermissions().contains(AccPermissions.MANAGE_FAMILY_GROUP);
        if (!allowed) {
            throw new AccessDeniedException("Você não tem permissão para " + action + " -- só o Family Manager pode.");
        }
    }

    // Categoria pessoal: só o dono (ou DEVELOPER) gerencia.
    // Categoria de grupo: só Family Manager (ou DEVELOPER) gerencia.
    private void requireManageCategory(Category category, Long requesterId, String action) {
        UserAccount requester = getRequester(requesterId);
        if (requester.getAccType() == AccType.DEVELOPER) {
            return;
        }

        if (category.getOwner() != null) {
            if (!category.getOwner().getId().equals(requesterId)) {
                throw new AccessDeniedException("Você não tem permissão para " + action + " -- categoria pessoal de outro usuário.");
            }
            return;
        }

        requireGroupManager(requester, action);
    }
}
