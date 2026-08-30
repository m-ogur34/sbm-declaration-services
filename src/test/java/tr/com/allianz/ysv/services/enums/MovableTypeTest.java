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
    @DisplayName("fromSbmValue rejects the OPUS numeric codes (SBM contract is string only)")
    void numericCodesAreNotAcceptedBySbmValue() {
        assertThatThrownBy(() -> MovableType.fromSbmValue("1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MovableType.fromSbmValue("2"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("fromExcel accepts both the numeric OPUS codes and the text form")
    void fromExcel_acceptsNumericAndText() {
        assertThat(MovableType.fromExcel("1")).isSameAs(MovableType.MENKUL);
        assertThat(MovableType.fromExcel("2")).isSameAs(MovableType.GAYRIMENKUL);
        assertThat(MovableType.fromExcel("1.0")).isSameAs(MovableType.MENKUL);
        assertThat(MovableType.fromExcel("2.0")).isSameAs(MovableType.GAYRIMENKUL);
        assertThat(MovableType.fromExcel(" menkul ")).isSameAs(MovableType.MENKUL);
        assertThat(MovableType.fromExcel("GAYRIMENKUL")).isSameAs(MovableType.GAYRIMENKUL);
    }

    @Test
    void fromExcel_rejectsUnknownAndNull() {
        assertThatThrownBy(() -> MovableType.fromExcel("3"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MovableType.fromExcel("TASIT"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MovableType.fromExcel(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
