package MatheusAPI.s.AI_FinanceApp.common;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Sem esse módulo, o Jackson tenta serializar o proxy do Hibernate (ByteBuddyInterceptor)
    // de qualquer relacionamento @ManyToOne/@OneToMany LAZY que ainda não foi carregado, e quebra
    // com "No serializer found for class ...ByteBuddyInterceptor".
    //
    // FORCE_LAZY_LOADING: em vez de simplesmente omitir o campo lazy não carregado do JSON,
    // força o Hibernate a buscar o dado de verdade na hora de serializar. Isso só funciona
    // porque spring.jpa.open-in-view está ligado (padrão do Spring Boot) -- a sessão do
    // Hibernate continua aberta até a resposta ser montada. Necessário aqui porque o
    // front depende de campos como user.group.id vindo preenchido de verdade.
    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, true);
        return module;
    }
}
