package MatheusAPI.s.AI_FinanceApp.ai;

import java.util.Map;

// parameters é um JSON Schema (tipo object) descrevendo os argumentos aceitos pela função.
public record GrokFunctionDef(String name, String description, Map<String, Object> parameters) {}
