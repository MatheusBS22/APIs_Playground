package MatheusAPI.s.AI_FinanceApp.category;

import MatheusAPI.s.AI_FinanceApp.balanceflow.FlowType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByGroupId(Long groupId);
    List<Category> findByGroupIdAndType(Long groupId, FlowType type);

    // Categorias do grupo (visíveis/usáveis por todo mundo do grupo).
    List<Category> findByGroupIdAndOwnerIsNull(Long groupId);

    // Categorias pessoais de um usuário específico dentro do grupo.
    List<Category> findByGroupIdAndOwnerId(Long groupId, Long ownerId);
}
