package tr.com.allianz.ysv.services.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

class RestClientConfigTest {

    private final RestClientConfig config = new RestClientConfig();

    @Test
    @DisplayName("client'lar Apache HttpClient 5 tabanlıdır (havuz + ayrı timeout kontrolü)")
    void requestFactory_isBackedByApacheHttpClient() {
        ClientHttpRequestFactory factory =
                RestClientConfig.requestFactory(Duration.ofSeconds(1), Duration.ofSeconds(2));

        assertThat(factory).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
    }

    @Test
    void esbRestClient_isBuiltFromTheEsbTimeouts() {
        EsbProperties properties = new EsbProperties();
        properties.setBaseUrl("http://esb.test.local:12000");
        properties.setConnectTimeout(Duration.ofSeconds(3));
        properties.setReadTimeout(Duration.ofSeconds(4));

        assertThat(config.esbRestClient(properties)).isNotNull();
        assertThat(properties.beyannameUrl())
                .isEqualTo("http://esb.test.local:12000/api/rest/vergi-beyan-rs/v10/ysv-beyanname");
        assertThat(properties.sorguUrl())
                .isEqualTo("http://esb.test.local:12000/api/rest/vergi-beyan-rs/v10/ysv-beyanname");
    }

    @Test
    void tokenRestClient_isBuiltFromTheTokenTimeouts() {
        TokenManagementProperties properties = new TokenManagementProperties();
        properties.setBaseUrl("http://token.test.local");
        properties.setPath("/alz-token-management/api/v1/tokens/sbm-token-generate");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(2));

        assertThat(config.tokenRestClient(properties)).isNotNull();
        assertThat(properties.tokenUrl())
                .isEqualTo("http://token.test.local/alz-token-management/api/v1/tokens/sbm-token-generate");
    }
}
