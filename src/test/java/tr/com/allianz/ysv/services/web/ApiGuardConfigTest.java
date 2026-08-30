package tr.com.allianz.ysv.services.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import tr.com.allianz.ysv.services.config.ApiGuardProperties;

class ApiGuardConfigTest {

    @Test
    void registersTheFilterForAllPathsWithHighPrecedence() {
        FilterRegistrationBean<ApiGuardFilter> registration =
                new ApiGuardConfig().apiGuardFilterRegistration(new ApiGuardProperties());

        assertThat(registration.getFilter()).isInstanceOf(ApiGuardFilter.class);
        assertThat(registration.getUrlPatterns()).containsExactly("/*");
        assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 10);
    }
}
