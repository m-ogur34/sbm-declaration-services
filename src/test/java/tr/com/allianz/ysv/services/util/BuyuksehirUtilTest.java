package tr.com.allianz.ysv.services.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import tr.com.allianz.ysv.services.enums.SbmErrorCode;
import tr.com.allianz.ysv.services.exception.SbmIntegrationException;

class BuyuksehirUtilTest {

    private static final Set<Integer> EXPECTED = Set.of(
            1, 6, 7, 9, 10, 16, 20, 21, 22, 25, 26, 27, 31, 33, 34, 35,
            38, 41, 42, 44, 45, 46, 48, 52, 54, 55, 59, 61, 63, 65);

    @Test
    @DisplayName("exactly 30 metropolitan city codes are configured")
    void metropolitanCityCodes_contains30Entries() {
        assertThat(BuyuksehirUtil.metropolitanCityCodes()).hasSize(30).isEqualTo(EXPECTED);
    }

    @Test
    void metropolitanCityCodes_isUnmodifiable() {
        Set<Integer> codes = BuyuksehirUtil.metropolitanCityCodes();
        assertThatThrownBy(() -> codes.add(99)).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 6, 7, 9, 10, 16, 20, 21, 22, 25, 26, 27, 31, 33, 34, 35,
            38, 41, 42, 44, 45, 46, 48, 52, 54, 55, 59, 61, 63, 65})
    void isBuyuksehir_returnsTrueForEveryMetropolitanCode(int cityCode) {
        assertThat(BuyuksehirUtil.isBuyuksehir(cityCode)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 8, 11, 47, 68, 80, 81})
    void isBuyuksehir_returnsFalseForOtherCodes(int cityCode) {
        assertThat(BuyuksehirUtil.isBuyuksehir(cityCode)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    void isBuyuksehir_returnsFalseForNull(Integer cityCode) {
        assertThat(BuyuksehirUtil.isBuyuksehir(cityCode)).isFalse();
    }

    @Test
    @DisplayName("metropolitan city: ilceKodu is dropped even when the database holds one")
    void resolveDistrictCode_metropolitanCity_returnsNull() {
        assertThat(BuyuksehirUtil.resolveDistrictCode(34, 0)).isNull();
        assertThat(BuyuksehirUtil.resolveDistrictCode(34, 1234)).isNull();
        assertThat(BuyuksehirUtil.resolveDistrictCode(34, null)).isNull();
    }

    @Test
    void resolveDistrictCode_regularCity_returnsDistrict() {
        assertThat(BuyuksehirUtil.resolveDistrictCode(2, 1425)).isEqualTo(1425);
    }

    @Test
    @DisplayName("regular city without a district is rejected with RISK-HAVUZU-00008")
    void resolveDistrictCode_regularCityWithoutDistrict_throws() {
        assertThatThrownBy(() -> BuyuksehirUtil.resolveDistrictCode(2, null))
                .isInstanceOf(SbmIntegrationException.class)
                .hasMessageContaining("ilçe kodu zorunludur");
        assertErrorCode(() -> BuyuksehirUtil.resolveDistrictCode(2, null),
                SbmErrorCode.RISK_HAVUZU_00008);
    }

    @Test
    @DisplayName("district code 0 is treated as missing for a regular city")
    void resolveDistrictCode_regularCityWithZeroDistrict_throws() {
        assertErrorCode(() -> BuyuksehirUtil.resolveDistrictCode(2, 0),
                SbmErrorCode.RISK_HAVUZU_00008);
    }

    @Test
    void resolveDistrictCode_missingCity_throws() {
        assertErrorCode(() -> BuyuksehirUtil.resolveDistrictCode(null, 1425),
                SbmErrorCode.RISK_HAVUZU_00006);
    }

    @Test
    @DisplayName("grouping collapses a metropolitan city to a single city level declaration")
    void groupingDistrictCode_metropolitanCity_isAlwaysNull() {
        assertThat(BuyuksehirUtil.groupingDistrictCode(34, 0)).isNull();
        assertThat(BuyuksehirUtil.groupingDistrictCode(34, null)).isNull();
        // an OPUS row that carries a district for a metropolitan city still groups by city
        assertThat(BuyuksehirUtil.groupingDistrictCode(34, 1707)).isNull();
    }

    @Test
    @DisplayName("grouping keeps a regular city's district and never throws")
    void groupingDistrictCode_regularCity_keepsTheDistrict() {
        assertThat(BuyuksehirUtil.groupingDistrictCode(2, 1425)).isEqualTo(1425);
    }

    @Test
    @DisplayName("null and 0 collapse to one group so unusable rows fail once, together")
    void groupingDistrictCode_regularCityWithoutDistrict_isNull() {
        assertThat(BuyuksehirUtil.groupingDistrictCode(2, 0)).isNull();
        assertThat(BuyuksehirUtil.groupingDistrictCode(2, null)).isNull();
        assertThat(BuyuksehirUtil.groupingDistrictCode(null, 0)).isNull();
    }

    private static void assertErrorCode(ThrowingCallable callable, SbmErrorCode expected) {
        Throwable thrown = catchThrowable(callable);
        assertThat(thrown).isInstanceOf(SbmIntegrationException.class);
        assertThat(((SbmIntegrationException) thrown).getErrorCode()).isEqualTo(expected.getCode());
    }
}
