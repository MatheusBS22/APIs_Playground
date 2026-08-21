package MatheusAPI.s.AI_FinanceApp.ai;

import MatheusAPI.s.AI_FinanceApp.account.Account;
import MatheusAPI.s.AI_FinanceApp.account.AccountService;
import MatheusAPI.s.AI_FinanceApp.balanceflow.BalanceFlow;
import MatheusAPI.s.AI_FinanceApp.balanceflow.BalanceFlowService;
import MatheusAPI.s.AI_FinanceApp.balanceflow.FlowType;
import MatheusAPI.s.AI_FinanceApp.common.AccessDeniedException;
import MatheusAPI.s.AI_FinanceApp.goal.Goal;
import MatheusAPI.s.AI_FinanceApp.goal.GoalProjection;
import MatheusAPI.s.AI_FinanceApp.goal.GoalService;
import MatheusAPI.s.AI_FinanceApp.user.AccType;
import MatheusAPI.s.AI_FinanceApp.user.UserAccount;
import MatheusAPI.s.AI_FinanceApp.user.UserAccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Funções que o Grok pode chamar (function calling) para consultar dados reais do usuário
// antes de responder. Cada função reaproveita os services existentes -- as checagens de
// permissão/posse de cada um continuam valendo, então a IA nunca vê mais do que o próprio
// usuário poderia ver navegando pelo app.
@Component
@RequiredArgsConstructor
public class AiFunctions {

    private final UserAccountService userAccountService;
    private final AccountService accountService;
    private final BalanceFlowService balanceFlowService;
    private final GoalService goalService;
    private final ObjectMapper objectMapper;

    public List<GrokTool> getToolDefinitions() {
        return List.of(
                tool("consultar_saldo",
                        "Consulta o saldo total do usuário e o saldo de cada carteira dele. Use sempre que a " +
                                "pergunta envolver saldo, quanto dinheiro a pessoa tem, ou o saldo de uma carteira específica.",
                        """
                        {"type":"object","properties":{
                          "carteira":{"type":["string","null"],"description":"Nome (ou parte do nome) de uma carteira específica, opcional. Se omitido, retorna todas as carteiras do usuário."}
                        }}
                        """),
                tool("analisar_gastos_por_categoria",
                        "Soma os lançamentos do usuário agrupados por categoria, do maior para o menor. Use para " +
                                "perguntas sobre onde a pessoa está gastando mais, ou gastos por categoria (alimentação, transporte, etc).",
                        """
                        {"type":"object","properties":{
                          "tipo":{"type":["string","null"],"enum":["EXPENSE","INCOME",null],"description":"Filtra só despesas (EXPENSE) ou só receitas (INCOME). Opcional -- se omitido, considera os dois."}
                        }}
                        """),
                tool("verificar_compra",
                        "Verifica se uma compra de determinado valor cabe no saldo disponível do usuário (ou de uma " +
                                "carteira específica) e classifica o risco em verde/amarelo/vermelho. Use para perguntas do tipo " +
                                "'posso comprar/gastar X reais'.",
                        """
                        {"type":"object","properties":{
                          "valor":{"type":"number","description":"Valor da compra em reais."},
                          "carteira":{"type":["string","null"],"description":"Nome da carteira a considerar, opcional. Se omitido, usa o saldo total do usuário."}
                        },"required":["valor"]}
                        """),
                tool("consultar_metas",
                        "Lista as metas financeiras visíveis ao usuário (metas do grupo familiar + metas individuais " +
                                "dele), com valor alvo, valor atual, quanto falta e quanto precisa guardar por mês para cumprir o prazo.",
                        """
                        {"type":"object","properties":{
                          "nome":{"type":["string","null"],"description":"Filtra pelo nome (ou parte do nome) da meta, opcional. Se omitido, lista todas."}
                        }}
                        """),
                tool("simular_aporte_meta",
                        "Simula em quantos meses uma meta será atingida se o usuário guardar um valor mensal " +
                                "proposto, e se isso cumpre o prazo da meta. Use consultar_metas antes para descobrir o id, se necessário.",
                        """
                        {"type":"object","properties":{
                          "metaId":{"type":["integer","null"],"description":"Id da meta (obtido via consultar_metas)."},
                          "nome":{"type":["string","null"],"description":"Nome da meta, alternativa ao metaId caso o id não seja conhecido."},
                          "aporteMensal":{"type":"number","description":"Valor mensal proposto, em reais."}
                        },"required":["aporteMensal"]}
                        """),
                tool("consultar_gastos_periodo",
                        "Soma o total de despesas ou receitas do usuário entre duas datas. Use para perguntas sobre " +
                                "quanto foi gasto ou recebido em um período específico (semana, mês, etc).",
                        """
                        {"type":"object","properties":{
                          "tipo":{"type":"string","enum":["EXPENSE","INCOME"]},
                          "dataInicio":{"type":"string","description":"Data inicial no formato AAAA-MM-DD."},
                          "dataFim":{"type":"string","description":"Data final no formato AAAA-MM-DD."}
                        },"required":["tipo","dataInicio","dataFim"]}
                        """)
        );
    }

    public Object execute(String name, JsonNode args, Long requesterId) {
        return switch (name) {
            case "consultar_saldo" -> consultarSaldo(args, requesterId);
            case "analisar_gastos_por_categoria" -> analisarGastosPorCategoria(args, requesterId);
            case "verificar_compra" -> verificarCompra(args, requesterId);
            case "consultar_metas" -> consultarMetas(args, requesterId);
            case "simular_aporte_meta" -> simularAporteMeta(args, requesterId);
            case "consultar_gastos_periodo" -> consultarGastosPeriodo(args, requesterId);
            default -> Map.of("erro", "Função desconhecida: " + name);
        };
    }

    // ---------- funções ----------

    private Object consultarSaldo(JsonNode args, Long requesterId) {
        UserAccount requester = userAccountService.getById(requesterId);
        Long groupId = requester.getGroup().getId();
        List<Account> accounts = accountService.listByGroup(groupId, requesterId);

        String filtro = textOrNull(args, "carteira");
        if (filtro != null) {
            String f = filtro.toLowerCase();
            accounts = accounts.stream().filter(a -> a.getName().toLowerCase().contains(f)).toList();
        }

        List<Map<String, Object>> carteiras = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Account a : accounts) {
            BigDecimal saldo = balanceFlowService.getBalanceByAccount(a.getId(), requesterId);
            total = total.add(saldo);
            carteiras.add(Map.of("carteira", a.getName(), "saldo", saldo));
        }

        return Map.of("saldoTotal", total, "carteiras", carteiras);
    }

    private Object analisarGastosPorCategoria(JsonNode args, Long requesterId) {
        UserAccount requester = userAccountService.getById(requesterId);
        Long groupId = requester.getGroup().getId();

        String tipoTexto = textOrNull(args, "tipo");
        FlowType tipo = tipoTexto == null ? null : FlowType.valueOf(tipoTexto.toUpperCase());

        List<BalanceFlow> flows = balanceFlowService.listByGroup(groupId, requesterId);
        if (tipo != null) {
            flows = flows.stream().filter(f -> f.getType() == tipo).toList();
        }

        Map<String, BigDecimal> porCategoria = flows.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getCategory().getName(),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, BalanceFlow::getAmount, BigDecimal::add)
                ));

        List<Map<String, Object>> ordenado = porCategoria.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> Map.<String, Object>of("categoria", e.getKey(), "total", e.getValue()))
                .toList();

        return Map.of("porCategoria", ordenado);
    }

    private Object verificarCompra(JsonNode args, Long requesterId) {
        if (!args.hasNonNull("valor")) {
            throw new IllegalArgumentException("Informe o valor da compra");
        }
        BigDecimal valor = new BigDecimal(args.get("valor").asText());

        UserAccount requester = userAccountService.getById(requesterId);
        Long groupId = requester.getGroup().getId();

        String carteira = textOrNull(args, "carteira");
        BigDecimal saldoAtual;
        if (carteira != null) {
            String f = carteira.toLowerCase();
            Account account = accountService.listByGroup(groupId, requesterId).stream()
                    .filter(a -> a.getName().toLowerCase().contains(f))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Carteira não encontrada: " + carteira));
            saldoAtual = balanceFlowService.getBalanceByAccount(account.getId(), requesterId);
        } else {
            saldoAtual = balanceFlowService.getBalanceByGroup(groupId, requesterId);
        }

        BigDecimal saldoDepois = saldoAtual.subtract(valor);
        boolean cabe = saldoDepois.signum() >= 0;

        // vermelho: estoura o saldo. amarelo: cabe, mas deixa menos de 20% do saldo atual. verde: sobra tranquilo.
        String status;
        if (!cabe) {
            status = "vermelho";
        } else if (saldoAtual.signum() > 0 && saldoDepois.compareTo(saldoAtual.multiply(new BigDecimal("0.2"))) < 0) {
            status = "amarelo";
        } else {
            status = "verde";
        }

        return Map.of(
                "saldoAtual", saldoAtual,
                "valorCompra", valor,
                "saldoDepoisDaCompra", saldoDepois,
                "cabeNoSaldo", cabe,
                "status", status
        );
    }

    private Object consultarMetas(JsonNode args, Long requesterId) {
        UserAccount requester = userAccountService.getById(requesterId);
        Long groupId = requester.getGroup().getId();

        List<Goal> metas = new ArrayList<>(goalService.listGroupGoals(groupId));
        metas.addAll(goalService.listByOwner(requesterId, requesterId));

        String filtro = textOrNull(args, "nome");
        if (filtro != null) {
            String f = filtro.toLowerCase();
            metas = metas.stream().filter(g -> g.getName().toLowerCase().contains(f)).toList();
        }

        List<Map<String, Object>> resultado = metas.stream().map(g -> {
            BigDecimal faltante = g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("nome", g.getName());
            m.put("tipo", g.getOwner() == null ? "grupo" : "individual");
            m.put("valorAlvo", g.getTargetAmount());
            m.put("valorAtual", g.getCurrentAmount());
            m.put("faltante", faltante);
            m.put("prazo", g.getDeadline());
            m.put("aporteMensalNecessario", goalService.getRequiredMonthlyContribution(g.getId()));
            return m;
        }).toList();

        return Map.of("metas", resultado);
    }

    private Object simularAporteMeta(JsonNode args, Long requesterId) {
        if (!args.hasNonNull("aporteMensal")) {
            throw new IllegalArgumentException("Informe o valor do aporte mensal");
        }
        BigDecimal aporte = new BigDecimal(args.get("aporteMensal").asText());

        UserAccount requester = userAccountService.getById(requesterId);
        Long groupId = requester.getGroup().getId();

        Goal goal = resolveGoal(args, groupId, requesterId);

        // Meta individual de outra pessoa -- IA não pode simular por conta própria.
        if (requester.getAccType() != AccType.DEVELOPER
                && goal.getOwner() != null
                && !goal.getOwner().getId().equals(requesterId)) {
            throw new AccessDeniedException("Você não pode simular aportes em uma meta individual de outro usuário");
        }

        GoalProjection projecao = goalService.simulateMonthlyContribution(goal.getId(), aporte);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("meta", goal.getName());
        resultado.put("mesesNecessarios", projecao.monthsNeeded());
        resultado.put("dataProjetada", projecao.projectedCompletionDate());
        resultado.put("dentroDoPrazo", projecao.onTrack());
        resultado.put("diferencaEmRelacaoAoAporteMinimo", projecao.monthlyDifference());
        return resultado;
    }

    private Object consultarGastosPeriodo(JsonNode args, Long requesterId) {
        if (!args.hasNonNull("tipo") || !args.hasNonNull("dataInicio") || !args.hasNonNull("dataFim")) {
            throw new IllegalArgumentException("Informe tipo, dataInicio e dataFim");
        }
        FlowType tipo = FlowType.valueOf(args.get("tipo").asText().toUpperCase());
        LocalDate inicio = LocalDate.parse(args.get("dataInicio").asText());
        LocalDate fim = LocalDate.parse(args.get("dataFim").asText());

        UserAccount requester = userAccountService.getById(requesterId);
        Long groupId = requester.getGroup().getId();

        BigDecimal total = balanceFlowService.getTotalByPeriod(
                groupId, tipo, inicio.atStartOfDay(), fim.atTime(LocalTime.MAX), requesterId);

        return Map.of("tipo", tipo, "dataInicio", inicio, "dataFim", fim, "total", total);
    }

    // ---------- helpers ----------

    private Goal resolveGoal(JsonNode args, Long groupId, Long requesterId) {
        if (args.hasNonNull("metaId")) {
            return goalService.getById(args.get("metaId").asLong());
        }
        String nome = textOrNull(args, "nome");
        if (nome == null) {
            throw new IllegalArgumentException("Informe o id ou o nome da meta");
        }
        String f = nome.toLowerCase();
        List<Goal> candidatas = new ArrayList<>(goalService.listGroupGoals(groupId));
        candidatas.addAll(goalService.listByOwner(requesterId, requesterId));
        return candidatas.stream()
                .filter(g -> g.getName().toLowerCase().contains(f))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada: " + nome));
    }

    private String textOrNull(JsonNode args, String field) {
        return args != null && args.hasNonNull(field) ? args.get(field).asText() : null;
    }

    private GrokTool tool(String name, String description, String parametersJsonSchema) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = objectMapper.readValue(parametersJsonSchema, Map.class);
            return GrokTool.function(new GrokFunctionDef(name, description, parameters));
        } catch (Exception e) {
            throw new IllegalStateException("Definição de ferramenta de IA inválida: " + name, e);
        }
    }
}
