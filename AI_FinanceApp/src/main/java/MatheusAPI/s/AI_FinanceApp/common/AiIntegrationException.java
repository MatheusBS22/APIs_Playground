package MatheusAPI.s.AI_FinanceApp.common;

// Lançada quando a chamada pra API de IA externa (Grok) falha -- timeout, chave inválida, resposta malformada etc.
public class AiIntegrationException extends RuntimeException {
    public AiIntegrationException(String message) {
        super(message);
    }

    public AiIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
