package tr.com.allianz.ysv.services.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void openApi_describesTheDeclarationApi() {
        OpenAPI openApi = new OpenApiConfig().sbmDeclarationOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("SBM Declaration Services");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openApi.getInfo().getDescription()).contains("SBM");
        assertThat(openApi.getInfo().getContact().getName()).isEqualTo("Allianz Sigorta - YSV");
    }
}
