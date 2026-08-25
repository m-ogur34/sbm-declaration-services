package tr.com.allianz.ysv.services.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI sbmDeclarationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SBM Declaration Services")
                        .version("v1")
                        .description("Yangın Sigorta Vergisi (YSV) beyannamelerinin ESB üzerinden "
                                + "SBM'ye gönderilmesi, güncellenmesi ve sorgulanması.")
                        .contact(new Contact().name("Allianz Sigorta - YSV")));
    }
}
