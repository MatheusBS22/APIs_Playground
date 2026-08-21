package MatheusAPI.s.AI_FinanceApp.ai;

import MatheusAPI.s.AI_FinanceApp.common.AiIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiFunctions aiFunctions;
    private final ObjectMapper objectMapper;

    @Value("${xai.api.key:}")
    private String apiKey;

    @Value("${xai.base-url:https://api.groq.com/openai/v1/chat/completions}")
    private String baseUrl;

    // llama-3.3-70b-versatile: modelo gratuito na Groq com suporte a function calling.
    // Verifique em https://console.groq.com quais modelos sua conta tem liberados.
    @Value("${xai.model:llama-3.3-70b-versatile}")
    private String model;

    // Evita loop infinito caso o modelo insista em chamar funções sem nunca concluir.
    private static final int MAX_TOOL_TURNS = 5;

    private static final String SYSTEM_PROMPT = """
            Você é o assistente financeiro do app SmartFin. Responda sempre em português do Brasil,
            de forma direta e curta.

            Você tem acesso a funções que consultam os dados financeiros reais do usuário (saldo,
            carteiras, categorias, metas, lançamentos por período). SEMPRE use as funções disponíveis
            para obter números exatos antes de responder perguntas sobre a situação financeira da
            pessoa -- nunca invente ou estime valores. Se, mesmo depois de consultar os dados, a
            informação pedida não existir, diga isso claramente em vez de chutar.

            Ao responder se uma compra "cabe no orçamento" (função verificar_compra), sinalize o
            resultado com um emoji: 🟢 status verde (cabe tranquilo), 🟡 status amarelo (cabe, mas
            aperta o orçamento), 🔴 status vermelho (não é recomendado agora).

            Perguntas de matemática pura (porcentagem, parcelamento, projeções simples) você pode
            calcular diretamente, sem precisar de função. Perguntas de educação financeira (o que é
            orçamento, reserva, etc.) você pode responder com seu próprio conhecimento.
            """;

    public String ask(String question, Long requesterId) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("A pergunta não pode ser vazia");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiIntegrationException("Chave da API de IA não configurada no servidor (GROQ_API_KEY ausente)");
        }

        List<GrokMessage> messages = new ArrayList<>();
        messages.add(GrokMessage.system(SYSTEM_PROMPT));
        messages.add(GrokMessage.user(question));

        List<GrokTool> tools = aiFunctions.getToolDefinitions();
        RestClient restClient = RestClient.create();

        for (int turn = 0; turn < MAX_TOOL_TURNS; turn++) {
            GrokChatRequest requestBody = new GrokChatRequest(model, messages, tools, "auto");
            GrokChatResponse response = callGrok(restClient, requestBody);

            GrokMessage assistantMessage = extractMessage(response);
            messages.add(assistantMessage);

            List<GrokToolCall> toolCalls = assistantMessage.getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                return assistantMessage.getContent() != null ? assistantMessage.getContent() : "";
            }

            for (GrokToolCall call : toolCalls) {
                messages.add(GrokMessage.tool(call.getId(), executeToolCall(call, requesterId)));
            }
        }

        throw new AiIntegrationException("A IA não conseguiu concluir a resposta (excesso de chamadas de função em sequência)");
    }

    private GrokChatResponse callGrok(RestClient restClient, GrokChatRequest body) {
        try {
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
            return response;
        } catch (RestClientException e) {
            throw new AiIntegrationException("Falha ao chamar a API de IA: " + e.getMessage(), e);
        }
    }

    private GrokMessage extractMessage(GrokChatResponse response) {
        GrokMessage message = response.choices().get(0).message();
        if (message == null) {
            throw new AiIntegrationException("A IA retornou uma resposta vazia");
        }
        return message;
    }

    // Executa a função pedida pelo modelo e serializa o resultado (ou o erro) como JSON,
    // que vira o conteúdo da mensagem "tool" devolvida pra IA continuar o raciocínio.
    private String executeToolCall(GrokToolCall call, Long requesterId) {
        try {
            JsonNode args = parseArguments(call.getFunction().getArguments());
            Object result = aiFunctions.execute(call.getFunction().getName(), args, requesterId);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return writeErrorSafely(e.getMessage());
        }
    }

    private JsonNode parseArguments(String rawArguments) throws Exception {
        if (rawArguments == null || rawArguments.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(rawArguments);
    }

    private String writeErrorSafely(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("erro", message != null ? message : "Erro desconhecido"));
        } catch (Exception e) {
            return "{\"erro\":\"Erro desconhecido\"}";
        }
    }
}
