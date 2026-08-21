package MatheusAPI.s.AI_FinanceApp.category;

import MatheusAPI.s.AI_FinanceApp.balanceflow.FlowType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<Category> create(@RequestBody CreateCategoryRequest request) {
        Category category = categoryService.create(request.name(), request.type(), request.groupId(), request.ownerId(), request.requesterId());
        return ResponseEntity.ok(category);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Category>> listByGroup(@PathVariable Long groupId, @RequestParam Long requesterId) {
        return ResponseEntity.ok(categoryService.listByGroup(groupId, requesterId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> update(@PathVariable Long id, @RequestBody UpdateCategoryNameRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request.name(), request.requesterId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long requesterId) {
        categoryService.delete(id, requesterId);
        return ResponseEntity.noContent().build();
    }
}

record CreateCategoryRequest(String name, FlowType type, Long groupId, Long ownerId, Long requesterId) {}
record UpdateCategoryNameRequest(String name, Long requesterId) {}
