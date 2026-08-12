package tr.com.allianz.ysv.services.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DistrictCodeResolverTest {

    @Test
    @DisplayName("0 means \"city level declaration\": the field is left out, never sent as 0")
    void resolve_zero_isDropped() {
        assertThat(DistrictCodeResolver.resolve(0)).isNull();
    }

    @Test
    void resolve_null_isDropped() {
        assertThat(DistrictCodeResolver.resolve(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 7, 1105, 1425, 1707, 99999})
    @DisplayName("any real district code is forwarded exactly as the database holds it")
    void resolve_realDistrict_isForwardedUnchanged(int districtCode) {
        assertThat(DistrictCodeResolver.resolve(districtCode)).isEqualTo(districtCode);
    }
}
