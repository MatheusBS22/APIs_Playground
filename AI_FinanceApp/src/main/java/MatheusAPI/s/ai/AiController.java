package MatheusAPI.s.AI_FinanceApp.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/ask")
    public ResponseEntity<AskAiResponse> ask(@RequestBody AskAiRequest request) {
        String answer = aiService.ask(request.question(), request.requesterId());
        return ResponseEntity.ok(new AskAiResponse(answer));
    }
}

record AskAiRequest(String question, Long requesterId) {}
record AskAiResponse(String answer) {}
