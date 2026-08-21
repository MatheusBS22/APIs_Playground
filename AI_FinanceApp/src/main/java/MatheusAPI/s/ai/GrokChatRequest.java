package MatheusAPI.s.AI_FinanceApp.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GrokChatRequest(
        String model,
        List<GrokMessage> messages,
        List<GrokTool> tools,
        @JsonProperty("tool_choice") String toolChoice
) {}
