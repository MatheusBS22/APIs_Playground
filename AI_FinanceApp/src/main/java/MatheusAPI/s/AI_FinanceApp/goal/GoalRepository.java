package MatheusAPI.s.AI_FinanceApp.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByGroupIdAndOwnerIsNull(Long groupId); // metas do grupo
    List<Goal> findByOwnerId(Long ownerId);                // metas individuais de um usuário
}
