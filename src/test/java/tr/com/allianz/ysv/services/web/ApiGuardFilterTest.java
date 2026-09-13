package tr.com.allianz.ysv.services.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tr.com.allianz.ysv.services.config.ApiGuardProperties;

class ApiGuardFilterTest {

    private ApiGuardProperties props(boolean enabled, String apiKey) {
        ApiGuardProperties p = new ApiGuardProperties();
        p.setEnabled(enabled);
        p.setApiKey(apiKey);
        return p;
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", uri);
        r.setRequestURI(uri);
        r.setRemoteAddr("10.0.0.5");
        return r;
    }

    private int run(ApiGuardFilter filter, MockHttpServletRequest req) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void disabled_passesEverythingThrough() throws Exception {
        ApiGuardFilter filter = new ApiGuardFilter(props(false, "secret"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("/api/v1/declarations/send"), res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void excludedPath_isNotChecked() throws Exception {
        ApiGuardFilter filter = new ApiGuardFilter(props(true, "secret"));

        assertThat(run(filter, request("/actuator/health"))).isEqualTo(200);
    }

    @Test
    @DisplayName("api-key tanımlıysa yanlış / eksik anahtar 401 döner")
    void wrongApiKey_isUnauthorized() throws Exception {
        ApiGuardFilter filter = new ApiGuardFilter(props(true, "secret"));

        assertThat(run(filter, request("/api/v1/declarations/send"))).isEqualTo(401);

        MockHttpServletRequest wrong = request("/api/v1/declarations/send");
        wrong.addHeader("X-Api-Key", "nope");
        assertThat(run(filter, wrong)).isEqualTo(401);
    }

    @Test
    void correctApiKey_passes() throws Exception {
        ApiGuardFilter filter = new ApiGuardFilter(props(true, "secret"));
        MockHttpServletRequest req = request("/api/v1/declarations/send");
        req.addHeader("X-Api-Key", "secret");

        assertThat(run(filter, req)).isEqualTo(200);
    }

    @Test
    @DisplayName("api-key boşsa anahtar kontrolü atlanır (uyarı bir kez loglanır, sonraki istekler de geçer)")
    void blankApiKey_skipsKeyCheck() throws Exception {
        ApiGuardFilter filter = new ApiGuardFilter(props(true, "  "));

        assertThat(run(filter, request("/api/v1/declarations/send"))).isEqualTo(200);
        assertThat(run(filter, request("/api/v1/declarations/send"))).isEqualTo(200);
    }

    @Test
    @DisplayName("context-path'li istekte muaf yol yine tanınır")
    void excludedPath_withContextPath() throws Exception {
        ApiGuardFilter filter = new ApiGuardFilter(props(true, "secret"));
        MockHttpServletRequest req = request("/sbm-declaration-services/actuator/health");
        req.setContextPath("/sbm-declaration-services");

        assertThat(run(filter, req)).isEqualTo(200);
    }

    @Test
    @DisplayName("kapasite dolunca 429 + Retry-After döner")
    void rateLimit_blocksAfterCapacity() throws Exception {
        ApiGuardProperties p = props(true, null);
        p.getRateLimit().setCapacity(2);
        p.getRateLimit().setRefillTokens(2);
        p.getRateLimit().setRefillPeriod(Duration.ofMinutes(10));
        ApiGuardFilter filter = new ApiGuardFilter(p);

        assertThat(run(filter, request("/api/v1/declarations/send"))).isEqualTo(200);
        assertThat(run(filter, request("/api/v1/declarations/send"))).isEqualTo(200);

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/declarations/send"), res, new MockFilterChain());
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isNotNull();
    }

    @Test
    void rateLimit_disabled_neverBlocks() throws Exception {
        ApiGuardProperties p = props(true, null);
        p.getRateLimit().setEnabled(false);
        ApiGuardFilter filter = new ApiGuardFilter(p);

        for (int i = 0; i < 50; i++) {
            assertThat(run(filter, request("/api/v1/declarations/send"))).isEqualTo(200);
        }
    }

    @Test
    void separateClients_haveSeparateBuckets() throws Exception {
        ApiGuardProperties p = props(true, null);
        p.getRateLimit().setCapacity(1);
        p.getRateLimit().setRefillTokens(1);
        p.getRateLimit().setRefillPeriod(Duration.ofMinutes(10));
        ApiGuardFilter filter = new ApiGuardFilter(p);

        MockHttpServletRequest a1 = request("/api/v1/declarations/send");
        a1.setRemoteAddr("10.0.0.1");
        MockHttpServletRequest b1 = request("/api/v1/declarations/send");
        b1.setRemoteAddr("10.0.0.2");
        MockHttpServletRequest a2 = request("/api/v1/declarations/send");
        a2.setRemoteAddr("10.0.0.1");

        assertThat(run(filter, a1)).isEqualTo(200);   // 10.0.0.1 ilk istek
        assertThat(run(filter, b1)).isEqualTo(200);   // 10.0.0.2 kendi kovası
        assertThat(run(filter, a2)).isEqualTo(429);   // 10.0.0.1 kovası boş
    }
}
