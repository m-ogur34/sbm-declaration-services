package tr.com.allianz.ysv.services.config;

import java.time.Duration;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * ESB ve {@code alz-token-management} çağrıları için {@link RestClient} bean'leri.
 *
 * <p>Alt taşıyıcı Apache HttpClient 5: bağlantı havuzu ve ayrı connect/read timeout
 * kontrolü sağlar. (Sorgu GET + query string ile yapılır, GET gövdesi kullanılmaz.)</p>
 *
 * <p>Client'lara base-url verilmez; servisler mutlak URL'i kendi ayarlarından kurar,
 * böylece hedef adres loglarda her zaman açıkça görünür.</p>
 */
@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    public static final String ESB_REST_CLIENT = "esbRestClient";
    public static final String TOKEN_REST_CLIENT = "tokenRestClient";

    @Bean(ESB_REST_CLIENT)
    public RestClient esbRestClient(EsbProperties properties) {
        return RestClient.builder()
                .requestFactory(requestFactory(properties.getConnectTimeout(), properties.getReadTimeout()))
                .build();
    }

    @Bean(TOKEN_REST_CLIENT)
    public RestClient tokenRestClient(TokenManagementProperties properties) {
        return RestClient.builder()
                .requestFactory(requestFactory(properties.getConnectTimeout(), properties.getReadTimeout()))
                .build();
    }

    /**
     * @param connectTimeout time allowed to establish the TCP connection
     * @param readTimeout    time allowed between the request and the full response
     * @return a request factory backed by a pooled Apache HttpClient 5 instance
     */
    static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()))
                .setSocketTimeout(Timeout.ofMilliseconds(readTimeout.toMillis()))
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout.toMillis()))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
