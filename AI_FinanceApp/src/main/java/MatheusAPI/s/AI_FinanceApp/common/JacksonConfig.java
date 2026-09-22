package MatheusAPI.s.AI_FinanceApp.common;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Sem esse módulo, o Jackson tenta serializar o proxy do Hibernate (ByteBuddyInterceptor)
    // de qualquer relacionamento @ManyToOne/@OneToMany LAZY que ainda não foi carregado, e quebra
    // com "No serializer found for class ...ByteBuddyInterceptor". Com o módulo registrado, ele
    // some do JSON quando não carregado (comportamento padrão), em vez de derrubar a resposta.
    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // Se preferir que o campo lazy não carregado apareça como null em vez de simplesmente
        // sumir do JSON, descomente a linha abaixo:
        // module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        return module;
    }
}
