package tr.com.allianz.ysv.services.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tr.com.allianz.ysv.services.dto.internal.SbmQueryRequest;

class JsonUtilTest {

    private JsonUtil jsonUtil;

    @BeforeEach
    void setUp() {
        jsonUtil = new JsonUtil(new ObjectMapper());
    }

    @Test
    void toJson_serializesObject() {
        String json = jsonUtil.toJson(SbmQueryRequest.builder()
                .sigortaSirketKodu("045")
                .ysvDosyaNo("YSV1")
                .build());
        assertThat(json).contains("\"sigortaSirketKodu\":\"045\"", "\"ysvDosyaNo\":\"YSV1\"");
    }

    @Test
    void toJson_returnsNullForNull() {
        assertThat(jsonUtil.toJson(null)).isNull();
    }

    @Test
    void toJson_returnsPlaceholderWhenSerializationFails() {
        assertThat(jsonUtil.toJson(new Unserializable())).contains("serialize edilemedi");
    }

    @Test
    void fromJson_parsesObject() {
        SbmQueryRequest parsed = jsonUtil.fromJson(
                "{\"sigortaSirketKodu\":\"045\",\"ysvDosyaNo\":\"YSV1\"}", SbmQueryRequest.class);
        assertThat(parsed).isNotNull();
        assertThat(parsed.getYsvDosyaNo()).isEqualTo("YSV1");
    }

    @Test
    void fromJson_returnsNullForNullOrBlank() {
        assertThat(jsonUtil.fromJson(null, SbmQueryRequest.class)).isNull();
        assertThat(jsonUtil.fromJson("  ", SbmQueryRequest.class)).isNull();
    }

    @Test
    void fromJson_returnsNullForMalformedJson() {
        assertThat(jsonUtil.fromJson("{not json", SbmQueryRequest.class)).isNull();
    }

    @Test
    void truncate_shortensOnlyWhenNeeded() {
        assertThat(JsonUtil.truncate(null, 5)).isNull();
        assertThat(JsonUtil.truncate("abc", 5)).isEqualTo("abc");
        assertThat(JsonUtil.truncate("abcdefgh", 5)).isEqualTo("abcde");
    }

    @Test
    @DisplayName("the limit is counted in bytes, so ERROR_DETAILS cannot overflow VARCHAR2(2000)")
    void truncate_countsBytesNotCharacters() {
        String turkish = "ş".repeat(10);

        String truncated = JsonUtil.truncate(turkish, 5);

        assertThat(truncated).isEqualTo("şş");
        assertThat(truncated.getBytes(StandardCharsets.UTF_8)).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("a cut landing inside a character drops it instead of leaving a broken byte")
    void truncate_neverSplitsAMultiByteCharacter() {
        assertThat(JsonUtil.truncate("aş", 2)).isEqualTo("a");
    }

    @Test
    void truncate_sbmReasonAtTheColumnLimit_staysWithinTheColumn() {
        String reasons = "Mükerrer beyanname kaydı mevcuttur. ".repeat(200);

        String truncated = JsonUtil.truncate(reasons, JsonUtil.ERROR_DETAILS_MAX_BYTES);

        assertThat(truncated.getBytes(StandardCharsets.UTF_8))
                .hasSizeLessThanOrEqualTo(JsonUtil.ERROR_DETAILS_MAX_BYTES);
        assertThat(truncated.length()).isLessThan(JsonUtil.ERROR_DETAILS_MAX_BYTES);
    }

    /** Jackson cannot serialize a bean without any accessible property. */
    static class Unserializable {
        private final String hidden = "x";

        String getHiddenInternal() {
            return hidden;
        }
    }
}
