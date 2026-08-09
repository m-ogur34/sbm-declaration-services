package tr.com.allianz.ysv.services.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MovableTypeTest {

    @Test
    @DisplayName("the enum only carries the string form SBM's REST contract expects")
    void sbmValuesMatchTheSbmContract() {
        assertThat(MovableType.values()).hasSize(2);
        assertThat(MovableType.MENKUL.getSbmValue()).isEqualTo("MENKUL");
        assertThat(MovableType.GAYRIMENKUL.getSbmValue()).isEqualTo("GAYRIMENKUL");
        assertThat(MovableType.valueOf("MENKUL")).isSameAs(MovableType.MENKUL);
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

    @Test
    @DisplayName("the OPUS numeric codes are not part of the enum any more")
    void numericCodesAreNotExposed() {
        assertThatThrownBy(() -> MovableType.fromSbmValue("1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MovableType.fromSbmValue("2"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
