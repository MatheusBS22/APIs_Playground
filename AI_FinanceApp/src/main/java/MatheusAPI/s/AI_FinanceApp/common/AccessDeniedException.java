package MatheusAPI.s.AI_FinanceApp.common;

// Lançada quando alguém pede uma ação em um dado que não é seu, ou sem a permissão exigida.
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
