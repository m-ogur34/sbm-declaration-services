package tr.com.allianz.ysv.services.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

    /** Jackson cannot serialize a bean without any accessible property. */
    static class Unserializable {
        private final String hidden = "x";

        String getHiddenInternal() {
            return hidden;
        }
    }
}
