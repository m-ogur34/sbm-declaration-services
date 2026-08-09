package tr.com.allianz.ysv.services.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaskUtilTest {

    @Test
    void mask_keepsFirstTenCharacters() {
        assertThat(MaskUtil.mask("eyJhbGciOiJIUzUxMiJ9.payload")).isEqualTo("eyJhbGciOi***");
    }

    @Test
    void mask_hidesShortValuesCompletely() {
        assertThat(MaskUtil.mask("8677399731")).isEqualTo("***");
        assertThat(MaskUtil.mask("")).isEqualTo("***");
    }

    @Test
    void mask_returnsNullForNull() {
        assertThat(MaskUtil.mask(null)).isNull();
    }
}
