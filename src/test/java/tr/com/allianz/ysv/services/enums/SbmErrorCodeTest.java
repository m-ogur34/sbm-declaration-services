package tr.com.allianz.ysv.services.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SbmErrorCodeTest {

    @ParameterizedTest
    @EnumSource(SbmErrorCode.class)
    void everyConstantIsResolvableAndDescribed(SbmErrorCode errorCode) {
        assertThat(errorCode.getCode()).isNotBlank();
        assertThat(errorCode.getDescription()).isNotBlank();
        assertThat(SbmErrorCode.fromCode(errorCode.getCode())).isSameAs(errorCode);
        assertThat(SbmErrorCode.find(errorCode.getCode())).contains(errorCode);
        assertThat(SbmErrorCode.describe(errorCode.getCode())).isEqualTo(errorCode.getDescription());
        assertThat(SbmErrorCode.valueOf(errorCode.name())).isSameAs(errorCode);
    }

    @ParameterizedTest
    @CsvSource({
            "RISK-HAVUZU-00002, RISK_HAVUZU_00002",
            "RISK-HAVUZU-00003, RISK_HAVUZU_00003",
            "RISK-HAVUZU-00004, RISK_HAVUZU_00004",
            "RISK-HAVUZU-00005, RISK_HAVUZU_00005",
            "RISK-HAVUZU-00006, RISK_HAVUZU_00006",
            "RISK-HAVUZU-00007, RISK_HAVUZU_00007",
            "RISK-HAVUZU-00008, RISK_HAVUZU_00008",
            "RISK-HAVUZU-00009, RISK_HAVUZU_00009",
            "SEC-00001, SEC_00001",
            "SEC-00002, SEC_00002",
            "SEC-00003, SEC_00003",
            "SEC-00004, SEC_00004",
            "SEC-00005, SEC_00005",
            "SEC-00006, SEC_00006",
            "SEC-00007, SEC_00007",
            "SEC-00008, SEC_00008",
            "CORE-00000, CORE_00000",
            "CORE-00001, CORE_00001",
            "CORE-00005, CORE_00005",
            "CORE-00006, CORE_00006",
            "CORE-00009, CORE_00009",
            "CORE-01000, CORE_01000",
            "CORE-01001, CORE_01001",
            "CORE-01004, CORE_01004",
            "CORE-01008, CORE_01008"
    })
    void everyDocumentedSbmCodeMapsToItsConstant(String wireCode, String constantName) {
        assertThat(SbmErrorCode.fromCode(wireCode)).isSameAs(SbmErrorCode.valueOf(constantName));
    }

    @Test
    void fromCode_normalizesCaseAndWhitespace() {
        assertThat(SbmErrorCode.fromCode("  sec-00002 ")).isSameAs(SbmErrorCode.SEC_00002);
    }

    @Test
    void fromCode_fallsBackToUnknownForUnmappedCodes() {
        assertThat(SbmErrorCode.fromCode("RISK-HAVUZU-99999")).isSameAs(SbmErrorCode.UNKNOWN);
        assertThat(SbmErrorCode.describe("RISK-HAVUZU-99999"))
                .isEqualTo(SbmErrorCode.UNKNOWN.getDescription());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void find_isEmptyForMissingCodes(String code) {
        assertThat(SbmErrorCode.find(code)).isEqualTo(Optional.empty());
        assertThat(SbmErrorCode.fromCode(code)).isSameAs(SbmErrorCode.UNKNOWN);
        assertThat(SbmErrorCode.isRetryableCode(code)).isFalse();
    }

    @Test
    void onlyExpiredTokenIsRetryable() {
        assertThat(SbmErrorCode.SEC_00002.isRetryable()).isTrue();
        assertThat(SbmErrorCode.isRetryableCode("SEC-00002")).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = SbmErrorCode.class, names = "SEC_00002", mode = EnumSource.Mode.EXCLUDE)
    void everyOtherCodeIsNotRetryable(SbmErrorCode errorCode) {
        assertThat(errorCode.isRetryable()).isFalse();
        assertThat(SbmErrorCode.isRetryableCode(errorCode.getCode())).isFalse();
    }

    @Test
    void unmappedCodeIsNotRetryable() {
        assertThat(SbmErrorCode.isRetryableCode("CORE-99999")).isFalse();
    }
}
