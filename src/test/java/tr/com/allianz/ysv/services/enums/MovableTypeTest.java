package tr.com.allianz.ysv.services.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MovableTypeTest {

    @Test
    void codesAndSbmValuesMatchTheSbmContract() {
        assertThat(MovableType.values()).hasSize(2);
        assertThat(MovableType.MENKUL.getCode()).isEqualTo(1);
        assertThat(MovableType.MENKUL.getSbmValue()).isEqualTo("MENKUL");
        assertThat(MovableType.GAYRIMENKUL.getCode()).isEqualTo(2);
        assertThat(MovableType.GAYRIMENKUL.getSbmValue()).isEqualTo("GAYRIMENKUL");
        assertThat(MovableType.valueOf("MENKUL")).isSameAs(MovableType.MENKUL);
    }

    @Test
    void fromCode_mapsOpusCodes() {
        assertThat(MovableType.fromCode(1)).isSameAs(MovableType.MENKUL);
        assertThat(MovableType.fromCode(2)).isSameAs(MovableType.GAYRIMENKUL);
    }

    @Test
    void fromCode_rejectsUnknownAndNull() {
        assertThatThrownBy(() -> MovableType.fromCode(3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MovableType.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromSbmValue_isCaseInsensitiveAndTrims() {
        assertThat(MovableType.fromSbmValue(" menkul ")).isSameAs(MovableType.MENKUL);
        assertThat(MovableType.fromSbmValue("GAYRIMENKUL")).isSameAs(MovableType.GAYRIMENKUL);
    }

    @Test
    void fromSbmValue_rejectsUnknownAndNull() {
        assertThatThrownBy(() -> MovableType.fromSbmValue("TASIT"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MovableType.fromSbmValue(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
