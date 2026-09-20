package MatheusAPI.s.AI_FinanceApp.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Exige token válido (Bearer) em toda escrita (POST/PUT/DELETE), com duas
// exceções: o login em si, e a criação de usuário (que é o próprio cadastro).
@Component
@RequiredArgsConstructor
public class AuthFilter implements Filter {

    private final TokenStore tokenStore;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String method = request.getMethod();
        String path = request.getRequestURI();
        boolean isWrite = method.equals("POST") || method.equals("PUT") || method.equals("DELETE");
        boolean isPublic = path.equals("/auth/login") || (path.equals("/users") && method.equals("POST"));

        if (isWrite && !isPublic) {
            String header = request.getHeader("Authorization");
            String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;

            if (token == null || tokenStore.resolve(token) == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Faça login antes dessa ação (token ausente ou inválido)\"}");
                return;
            }
        }

        chain.doFilter(req, res);
    }
}
