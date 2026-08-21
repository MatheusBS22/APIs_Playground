package MatheusAPI.s.AI_FinanceApp.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrokToolCallFunction {
    private String name;
    private String arguments; // JSON cru, precisa ser parseado antes de usar
}
