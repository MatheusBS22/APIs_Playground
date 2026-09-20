package MatheusAPI.s.AI_FinanceApp.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Token em memória -- simples e suficiente pro escopo atual.
// Trade-off aceito: todo mundo precisa logar de novo se o serviço reiniciar
// (no plano free do Render isso acontece de vez em quando).
@Component
public class TokenStore {

    private final Map<String, Long> tokensToUserId = new ConcurrentHashMap<>();

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        tokensToUserId.put(token, userId);
        return token;
    }

    public Long resolve(String token) {
        return tokensToUserId.get(token);
    }
}
