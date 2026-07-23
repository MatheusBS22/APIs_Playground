package MatheusAPI.s.AI_FinanceApp.category;

import MatheusAPI.s.AI_FinanceApp.balanceflow.FlowType;
import MatheusAPI.s.AI_FinanceApp.group.Group;
import MatheusAPI.s.AI_FinanceApp.group.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final GroupRepository groupRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Category create(String name, FlowType type, Long groupId) {

        Category category = new Category();
        category.setName(name);
        category.setType(type);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado: " + groupId));

        category.setGroup(group);

        return categoryRepository.save(category);
    }

    public List<Category> listByGroup(Long groupId) {
        return categoryRepository.findByGroupId(groupId);
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + id));
    }

    @Transactional
    public Category update(Long id, String newName) {
        Category category = getById(id);
        category.setName(newName);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getById(id);
        categoryRepository.delete(category);
    }
}
