package tr.com.allianz.ysv.services.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import tr.com.allianz.ysv.services.config.ApiGuardProperties;

/**
 * {@link ApiGuardFilter}'ı servlet zincirine kaydeder (rate limit + API key kontrolü).
 * Filtre en başta çalışsın diye yüksek öncelik verilir.
 */
@Configuration(proxyBeanMethods = false)
public class ApiGuardConfig {

    @Bean
    public FilterRegistrationBean<ApiGuardFilter> apiGuardFilterRegistration(ApiGuardProperties properties) {
        FilterRegistrationBean<ApiGuardFilter> registration =
                new FilterRegistrationBean<>(new ApiGuardFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("apiGuardFilter");
        return registration;
    }
}
