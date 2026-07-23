package MatheusAPI.s.AI_FinanceApp.group;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;

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
    public Group update(Long id, String newName) {
        Group group = getById(id);
        group.setName(newName);
        return groupRepository.save(group);
    }

    @Transactional
    public void delete(Long id) {
        Group group = getById(id);
        groupRepository.delete(group);
    }

}
