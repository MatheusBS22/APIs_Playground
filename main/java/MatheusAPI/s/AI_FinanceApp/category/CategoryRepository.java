package MatheusAPI.s.AI_FinanceApp.category;

import MatheusAPI.s.AI_FinanceApp.balanceflow.FlowType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByGroupId(Long groupId);
    List<Category> findByGroupIdAndType(Long groupId, FlowType type);
}
