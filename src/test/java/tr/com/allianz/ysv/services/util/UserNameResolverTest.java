package tr.com.allianz.ysv.services.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserNameResolverTest {

    @Test
    void resolve_null_staysNull() {
        assertThat(UserNameResolver.resolve(null)).isNull();
    }

    @Test
    void resolve_trimsSurroundingWhitespace() {
        assertThat(UserNameResolver.resolve("  WDA2422_1617 ")).isEqualTo("WDA2422_1617");
    }

    @Test
    void resolve_valueThatFits_isForwardedUnchanged() {
        String maxLength = "u".repeat(UserNameResolver.MAX_BYTES);

        assertThat(UserNameResolver.resolve(maxLength)).isEqualTo(maxLength);
    }

    @Test
    @DisplayName("an oversized header is refused here, not by Oracle with ORA-12899")
    void resolve_tooLong_isRefused() {
        String tooLong = "u".repeat(UserNameResolver.MAX_BYTES + 1);

        assertThatThrownBy(() -> UserNameResolver.resolve(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X-User-Name");
    }

    @Test
    @DisplayName("the limit counts bytes: 51 Turkish characters already exceed 100 bytes")
    void resolve_turkishCharactersCountAsTwoBytes() {
        assertThatThrownBy(() -> UserNameResolver.resolve("ş".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(UserNameResolver.resolve("ş".repeat(50))).hasSize(50);
    }
}
