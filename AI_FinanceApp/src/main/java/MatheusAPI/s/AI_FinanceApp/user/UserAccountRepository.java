package MatheusAPI.s.AI_FinanceApp.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    List<UserAccount> findByGroupId(Long groupId);
    long countByGroupId(Long groupId);
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<UserAccount> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);
}
