package MatheusAPI.s.AI_FinanceApp.ai;

public record GrokTool(String type, GrokFunctionDef function) {
    public static GrokTool function(GrokFunctionDef def) {
        return new GrokTool("function", def);
    }
}
