package tr.com.allianz.ysv.services.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogLevelTest {

    @Test
    void enumSurfaceMatchesTheColumnComment() {
        assertThat(LogLevel.values())
                .containsExactly(LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR);
        assertThat(LogLevel.valueOf("ERROR")).isSameAs(LogLevel.ERROR);
    }
}
