package tr.com.allianz.ysv.services.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateUtilTest {

    @Test
    void format_rendersSbmPattern() {
        assertThat(DateUtil.format(LocalDate.of(2026, 1, 20))).isEqualTo("2026-01-20");
    }

    @Test
    void format_returnsNullForNull() {
        assertThat(DateUtil.format(null)).isNull();
    }

    @Test
    void parse_readsSbmPattern() {
        assertThat(DateUtil.parse(" 2026-01-20 ")).isEqualTo(LocalDate.of(2026, 1, 20));
    }

    @Test
    void parse_returnsNullForNullOrBlank() {
        assertThat(DateUtil.parse(null)).isNull();
        assertThat(DateUtil.parse("   ")).isNull();
    }

    @Test
    void parse_returnsNullForUnparsableText() {
        assertThat(DateUtil.parse("20.01.2026")).isNull();
    }
}
