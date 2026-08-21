package MatheusAPI.s.AI_FinanceApp.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// ignoreUnknown: a resposta da xAI traz outros campos (id, model, usage, created...)
// que a gente não precisa mapear.
@JsonIgnoreProperties(ignoreUnknown = true)
public record GrokChatResponse(List<GrokChoice> choices) {}
