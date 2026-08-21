package MatheusAPI.s.AI_FinanceApp.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Mensagem no formato de chat da xAI (compatível com o formato OpenAI).
// Um único tipo cobre os 4 papéis (system/user/assistant/tool) -- os campos
// que não se aplicam a um papel ficam null e somem do JSON (@JsonInclude NON_NULL).
// ignoreUnknown: tolera campos extras que a xAI mande na mensagem de resposta (ex. refusal).
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrokMessage {

    private String role;
    private String content;

    // Presente em mensagens "assistant" quando o modelo decide chamar funções.
    @JsonProperty("tool_calls")
    private List<GrokToolCall> toolCalls;

    // Presente em mensagens "tool" -- identifica a qual chamada essa resposta se refere.
    @JsonProperty("tool_call_id")
    private String toolCallId;

    public GrokMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static GrokMessage system(String content) {
        return new GrokMessage("system", content);
    }

    public static GrokMessage user(String content) {
        return new GrokMessage("user", content);
    }

    public static GrokMessage tool(String toolCallId, String content) {
        GrokMessage message = new GrokMessage("tool", content);
        message.setToolCallId(toolCallId);
        return message;
    }
}
