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

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final GroupRepository groupRepository;
    private final CategoryRepository categoryRepository;
    private final UserAccountRepository userAccountRepository;

    // Categoria é recurso do grupo -- só quem administra o grupo (ou DEVELOPER) mexe.
    @Transactional
    public Category create(String name, FlowType type, Long groupId, Long requesterId) {
        requireGroupManager(requesterId, "criar uma categoria");

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado: " + groupId));

        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setGroup(group);

        return categoryRepository.save(category);
    }

    // Listar/ver categorias é liberado pra todo mundo do grupo -- é só visualização.
    public List<Category> listByGroup(Long groupId) {
        return categoryRepository.findByGroupId(groupId);
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + id));
    }

    @Transactional
    public Category update(Long id, String newName, Long requesterId) {
        requireGroupManager(requesterId, "editar essa categoria");
        Category category = getById(id);
        category.setName(newName);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        requireGroupManager(requesterId, "apagar essa categoria");
        Category category = getById(id);
        categoryRepository.delete(category);
    }

    private void requireGroupManager(Long requesterId, String action) {
        UserAccount requester = userAccountRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + requesterId));
        boolean allowed = requester.getAccType() == AccType.DEVELOPER
                || requester.getAccPermissions().contains(AccPermissions.MANAGE_FAMILY_GROUP);
        if (!allowed) {
            throw new AccessDeniedException("Você não tem permissão para " + action + " -- só o Family Manager pode.");
        }
    }
}
