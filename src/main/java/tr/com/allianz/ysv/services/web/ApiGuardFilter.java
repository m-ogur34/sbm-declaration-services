package tr.com.allianz.ysv.services.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tr.com.allianz.ysv.services.config.ApiGuardProperties;

/**
 * PEN test savunma katmanı — her HTTP isteğinden önce çalışır.
 *
 * <p>Sırasıyla: (1) muaf yol mu / katman kapalı mı → geç, (2) {@code X-Api-Key} header'ı
 * beklenen değerle eşleşiyor mu → değilse 401, (3) istemci (API key ya da IP) için istek
 * hızı kovası dolu mu → değilse 429 + {@code Retry-After}. Kovalar bellek içindedir; çok
 * replica'da her instance kendi sayacını tutar (bkz. CALISMA-PRENSIBI.md §14).</p>
 *
 * <p>Bean değildir; {@code ApiGuardConfig} bir {@code FilterRegistrationBean} ile kaydeder.
 * Böylece {@code @WebMvcTest} slice'ları bu filtreyi otomatik yüklemez.</p>
 */
@Slf4j
public class ApiGuardFilter extends OncePerRequestFilter {

    private final ApiGuardProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicBoolean missingKeyWarned = new AtomicBoolean(false);

    public ApiGuardFilter(ApiGuardProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        for (String pattern : properties.getExcludedPaths()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String presentedKey = request.getHeader(properties.getApiKeyHeader());

        if (!apiKeyValid(presentedKey)) {
            write(response, HttpStatus.UNAUTHORIZED, "API anahtarı eksik veya geçersiz.", null);
            return;
        }

        String clientId = (presentedKey != null && !presentedKey.isBlank())
                ? "key:" + presentedKey
                : "ip:" + request.getRemoteAddr();

        if (!allow(clientId)) {
            long retryAfter = Math.max(1, retryAfterSeconds());
            write(response, HttpStatus.TOO_MANY_REQUESTS,
                    "İstek hızı sınırı aşıldı. " + retryAfter + " sn sonra tekrar deneyin.",
                    retryAfter);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean apiKeyValid(String presentedKey) {
        String expected = properties.getApiKey();
        if (expected == null || expected.isBlank()) {
            if (missingKeyWarned.compareAndSet(false, true)) {
                log.warn("api-guard.api-key tanımlı değil — API anahtarı kontrolü DEVRE DIŞI. "
                        + "Ortam secret'ından bir değer verilmeli.");
            }
            return true;
        }
        return expected.equals(presentedKey);
    }

    private boolean allow(String clientId) {
        ApiGuardProperties.RateLimit rl = properties.getRateLimit();
        if (!rl.isEnabled()) {
            return true;
        }
        long refillNanos = Math.max(1L, rl.getRefillPeriod().toNanos());
        return buckets
                .computeIfAbsent(clientId, k -> new TokenBucket(rl.getCapacity(), rl.getRefillTokens(), refillNanos))
                .tryConsume();
    }

    private long retryAfterSeconds() {
        ApiGuardProperties.RateLimit rl = properties.getRateLimit();
        double perToken = (double) rl.getRefillPeriod().toSeconds() / Math.max(1, rl.getRefillTokens());
        return (long) Math.ceil(perToken);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message, Long retryAfter)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (retryAfter != null) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        }
        response.getWriter().write(
                "{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                        + "\",\"message\":\"" + message + "\"}");
    }

    /**
     * Basit jeton kovası (token-bucket). Ek bağımlılık istememek için elle yazıldı;
     * çağrılar arasında geçen süreye göre orantılı jeton doldurur.
     */
    static final class TokenBucket {

        private final double capacity;
        private final double refillTokens;
        private final double refillPeriodNanos;
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(double capacity, double refillTokens, double refillPeriodNanos) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriodNanos = refillPeriodNanos;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1d) {
                tokens -= 1d;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsed = now - lastRefillNanos;
            if (elapsed <= 0) {
                return;
            }
            double added = elapsed / refillPeriodNanos * refillTokens;
            if (added > 0) {
                tokens = Math.min(capacity, tokens + added);
                lastRefillNanos = now;
            }
        }
    }
}
