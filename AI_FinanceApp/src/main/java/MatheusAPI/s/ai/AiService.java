package MatheusAPI.s.AI_FinanceApp.ai;

import MatheusAPI.s.AI_FinanceApp.account.Account;
import MatheusAPI.s.AI_FinanceApp.account.AccountService;
import MatheusAPI.s.AI_FinanceApp.balanceflow.BalanceFlow;
import MatheusAPI.s.AI_FinanceApp.balanceflow.BalanceFlowService;
import MatheusAPI.s.AI_FinanceApp.common.AiIntegrationException;
import MatheusAPI.s.AI_FinanceApp.goal.Goal;
import MatheusAPI.s.AI_FinanceApp.goal.GoalService;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import MatheusAPI.s.AI_FinanceApp.user.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private final UserAccountService userAccountService;
    private final AccountService accountService;
    private final BalanceFlowService balanceFlowService;
    private final GoalService goalService;

    @Value("${xai.api.key:}")
    private String apiKey;

    @Value("${xai.base-url:https://api.x.ai/v1/chat/completions}")
    private String baseUrl;

    @Value("${xai.model:grok-4.6}")
    private String model;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String SYSTEM_PROMPT = """
            Você é o assistente financeiro do app Bora Brincar. Responda em português do Brasil,
            de forma direta e curta. Use os dados fornecidos no contexto para responder com precisão
            sobre saldo, gastos e metas do usuário. Se o contexto não tiver a informação necessária,
            diga isso claramente em vez de inventar números.
            """;

    public String ask(String question, Long requesterId) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("A pergunta não pode ser vazia");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiIntegrationException("Chave da API de IA não configurada no servidor (XAI_API_KEY ausente)");
        }

        String context = buildFinancialContext(requesterId);
        String userMessage = context + "\n\nPergunta do usuário: " + question;

        GrokChatRequest body = new GrokChatRequest(
                model,
                List.of(
                        new GrokMessage("system", SYSTEM_PROMPT),
                        new GrokMessage("user", userMessage)
                )
        );

        try {
            RestClient restClient = RestClient.create();
            GrokChatResponse response = restClient.post()
                    .uri(baseUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(GrokChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AiIntegrationException("A IA não retornou nenhuma resposta");
            }
            return response.choices().get(0).message().content();

        } catch (RestClientException e) {
            throw new AiIntegrationException("Falha ao chamar a API de IA: " + e.getMessage(), e);
        }
    }

    // Monta um resumo textual da situação financeira do usuário para dar contexto à IA.
    // Reaproveita os services existentes -- as checagens de permissão de cada um já se aplicam aqui.
    private String buildFinancialContext(Long requesterId) {
        UserAccount requester = userAccountService.getById(requesterId);
        Long groupId = requester.getGroup().getId();

        List<Account> accounts = accountService.listByGroup(groupId, requesterId);
        var balance = balanceFlowService.getBalanceByGroup(groupId, requesterId);
        List<BalanceFlow> recentFlows = balanceFlowService.listByGroup(groupId, requesterId).stream()
                .sorted(Comparator.comparing(BalanceFlow::getCreateTime).reversed())
                .limit(10)
                .toList();
        List<Goal> groupGoals = goalService.listGroupGoals(groupId);
        List<Goal> ownGoals = goalService.listByOwner(requesterId, requesterId);

        StringBuilder sb = new StringBuilder();
        sb.append("Contexto financeiro de ").append(requester.getUsername()).append(":\n");
        sb.append("Saldo total: R$ ").append(balance).append("\n");

        sb.append("Carteiras (").append(accounts.size()).append("): ");
        sb.append(accounts.stream().map(Account::getName).reduce((a, b) -> a + ", " + b).orElse("nenhuma"));
        sb.append("\n");

        sb.append("Últimos lançamentos:\n");
        if (recentFlows.isEmpty()) {
            sb.append("- nenhum lançamento ainda\n");
        } else {
            for (BalanceFlow f : recentFlows) {
                sb.append("- [").append(f.getCreateTime().format(DATE_FMT)).append("] ")
                        .append(f.getType()).append(" R$ ").append(f.getAmount())
                        .append(" \"").append(f.getTitle()).append("\"")
                        .append(" categoria: ").append(f.getCategory().getName())
                        .append("\n");
            }
        }

        sb.append("Metas do grupo (").append(groupGoals.size()).append("): ");
        sb.append(groupGoals.stream().map(Goal::getName).reduce((a, b) -> a + ", " + b).orElse("nenhuma"));
        sb.append("\n");

        sb.append("Metas individuais de ").append(requester.getUsername()).append(" (").append(ownGoals.size()).append("): ");
        sb.append(ownGoals.stream().map(Goal::getName).reduce((a, b) -> a + ", " + b).orElse("nenhuma"));
        sb.append("\n");

        return sb.toString();
    }
}

record GrokMessage(String role, String content) {}
record GrokChatRequest(String model, List<GrokMessage> messages) {}
record GrokChoice(GrokMessage message) {}
record GrokChatResponse(List<GrokChoice> choices) {}
