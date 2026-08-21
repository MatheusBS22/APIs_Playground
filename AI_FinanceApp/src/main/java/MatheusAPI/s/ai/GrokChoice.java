package MatheusAPI.s.AI_FinanceApp.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// ignoreUnknown: ignora finish_reason, index, logprobs etc. -- só a mensagem importa aqui.
@JsonIgnoreProperties(ignoreUnknown = true)
public record GrokChoice(GrokMessage message) {}
