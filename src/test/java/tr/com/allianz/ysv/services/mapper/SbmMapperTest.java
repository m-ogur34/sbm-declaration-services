package tr.com.allianz.ysv.services.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static tr.com.allianz.ysv.services.testsupport.DeclarationProcessFixtures.baseRow;
import static tr.com.allianz.ysv.services.testsupport.DeclarationProcessFixtures.districtRow;
import static tr.com.allianz.ysv.services.testsupport.DeclarationProcessFixtures.cityLevelRow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tr.com.allianz.ysv.services.dto.internal.SbmAmountItem;
import tr.com.allianz.ysv.services.dto.internal.SbmDeclarationRequest;
import tr.com.allianz.ysv.services.dto.internal.SbmQueryRequest;
import tr.com.allianz.ysv.services.entity.DeclarationProcess;
import tr.com.allianz.ysv.services.enums.MovableType;
import tr.com.allianz.ysv.services.enums.SbmErrorCode;
import tr.com.allianz.ysv.services.exception.SbmIntegrationException;

class SbmMapperTest {

    private static final String COMPANY_CODE = "045";

    private SbmMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mapper = new SbmMapper();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // --- POST -------------------------------------------------------------------------

    @Test
    @DisplayName("POST carries ay/yil/ilKodu and drops ilceKodu when the column holds 0")
    void toSendRequest_cityLevelDeclaration_omitsDistrictCode() {
        List<DeclarationProcess> group = List.of(
                cityLevelRow(1L, MovableType.MENKUL),
                cityLevelRow(2L, MovableType.GAYRIMENKUL));

        SbmDeclarationRequest request = mapper.toSendRequest(group, COMPANY_CODE);

        assertThat(request.getAy()).isEqualTo(1);
        assertThat(request.getYil()).isEqualTo(2026);
        assertThat(request.getIlKodu()).isEqualTo(1);
        assertThat(request.getIlceKodu()).isNull();
        assertThat(request.getSigortaSirketKodu()).isEqualTo(COMPANY_CODE);
        assertThat(request.getYsvDosyaNo()).isEqualTo("YSV202513491");
        assertThat(request.getYsvTutarList()).hasSize(2)
                .extracting(SbmAmountItem::getMenkulTipi)
                .containsExactly("MENKUL", "GAYRIMENKUL");
    }

    @Test
    void toSendRequest_districtDeclaration_sendsDistrictCodeUnchanged() {
        SbmDeclarationRequest request =
                mapper.toSendRequest(List.of(districtRow(1L, MovableType.MENKUL)), COMPANY_CODE);

        assertThat(request.getIlKodu()).isEqualTo(2);
        assertThat(request.getIlceKodu()).isEqualTo(1425);
    }

    @Test
    @DisplayName("a district for a city SBM treats as büyükşehir is forwarded, not blocked")
    void toSendRequest_districtIsNeverValidatedAgainstTheCity() {
        // 34 (İstanbul) is a metropolitan city; SBM will answer RISK-HAVUZU-00007 and that
        // answer is what lands in ERROR_DETAILS. The application does not second guess it.
        DeclarationProcess row = baseRow(1L, MovableType.MENKUL)
                .cityCode(34).districtCode(1707).build();

        SbmDeclarationRequest request = mapper.toSendRequest(List.of(row), COMPANY_CODE);

        assertThat(request.getIlKodu()).isEqualTo(34);
        assertThat(request.getIlceKodu()).isEqualTo(1707);
    }

    @Test
    @DisplayName("a city without a district is sent as is; SBM decides with RISK-HAVUZU-00008")
    void toSendRequest_missingDistrictIsNotRejectedLocally() {
        DeclarationProcess zeroDistrict = baseRow(1L, MovableType.MENKUL)
                .cityCode(2).districtCode(0).build();
        DeclarationProcess nullDistrict = baseRow(2L, MovableType.GAYRIMENKUL)
                .cityCode(2).districtCode(null).build();

        SbmDeclarationRequest request =
                mapper.toSendRequest(List.of(zeroDistrict, nullDistrict), COMPANY_CODE);

        assertThat(request.getIlKodu()).isEqualTo(2);
        assertThat(request.getIlceKodu()).isNull();
        assertThat(request.getYsvTutarList()).hasSize(2);
    }

    @Test
    void toSendRequest_missingCityCode_throws() {
        DeclarationProcess row = baseRow(1L, MovableType.MENKUL).cityCode(null).districtCode(5).build();

        assertSbmError(() -> mapper.toSendRequest(List.of(row), COMPANY_CODE),
                SbmErrorCode.CORE_01000);
    }

    @Test
    @DisplayName("ilceKodu is left out of the body instead of being sent as null or 0")
    void toSendRequest_serializedBodyOmitsMissingDistrict() throws Exception {
        SbmDeclarationRequest request =
                mapper.toSendRequest(List.of(cityLevelRow(1L, MovableType.MENKUL)), COMPANY_CODE);

        String json = objectMapper.writeValueAsString(request);

        assertThat(json).contains("\"ay\":1", "\"yil\":2026", "\"ilKodu\":1",
                "\"sonOdemeTarihi\":\"2026-01-20\"");
        assertThat(json).doesNotContain("ilceKodu");
    }

    @Test
    @DisplayName("ilKodu and ilceKodu are JSON numbers, never zero padded strings")
    void toSendRequest_cityAndDistrictAreUnpaddedNumbers() throws Exception {
        DeclarationProcess row = baseRow(1L, MovableType.MENKUL)
                .cityCode(2).districtCode(7).build();

        String json = objectMapper.writeValueAsString(mapper.toSendRequest(List.of(row), COMPANY_CODE));

        assertThat(json).contains("\"ilKodu\":2", "\"ilceKodu\":7", "\"ay\":1", "\"yil\":2026");
        assertThat(json).doesNotContain("\"ilKodu\":\"", "\"ilceKodu\":\"", "\"ay\":\"",
                "\"02\"", "\"07\"");
    }

    @Test
    @DisplayName("menkulTipi goes out as the SBM string, never as the OPUS numeric code")
    void toSendRequest_movableTypeIsAlwaysAString() throws Exception {
        List<DeclarationProcess> group = List.of(
                cityLevelRow(1L, MovableType.MENKUL),
                cityLevelRow(2L, MovableType.GAYRIMENKUL));

        String json = objectMapper.writeValueAsString(mapper.toSendRequest(group, COMPANY_CODE));

        assertThat(json).contains("\"menkulTipi\":\"MENKUL\"", "\"menkulTipi\":\"GAYRIMENKUL\"");
        assertThat(json).doesNotContain("\"menkulTipi\":1", "\"menkulTipi\":2",
                "\"menkulTipi\":\"1\"", "\"menkulTipi\":\"2\"");
    }

    @Test
    @DisplayName("every field keeps the JSON type the SBM contract and the WSDL agree on")
    void serializedTypesMatchTheSbmContract() throws Exception {
        DeclarationProcess row = baseRow(1L, MovableType.MENKUL)
                .cityCode(34)
                .districtCode(1425)
                .prevMonthRefundAmount(new BigDecimal("125.50"))
                .build();

        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(mapper.toSendRequest(List.of(row), COMPANY_CODE)));

        assertThat(json.get("ay").isInt()).as("ay").isTrue();
        assertThat(json.get("yil").isInt()).as("yil").isTrue();
        assertThat(json.get("ilKodu").isInt()).as("ilKodu").isTrue();
        assertThat(json.get("ilceKodu").isInt()).as("ilceKodu").isTrue();
        assertThat(json.get("sigortaSirketKodu").isTextual()).as("sigortaSirketKodu").isTrue();
        assertThat(json.get("ysvDosyaNo").isTextual()).as("ysvDosyaNo").isTrue();
        assertThat(json.get("sonOdemeTarihi").isTextual()).as("sonOdemeTarihi").isTrue();
        assertThat(json.get("sonOdemeTarihi").asText()).isEqualTo("2026-01-20");

        JsonNode item = json.get("ysvTutarList").get(0);
        assertThat(item.get("menkulTipi").isTextual()).as("menkulTipi").isTrue();
        assertThat(item.get("vergiOrani").isInt()).as("vergiOrani").isTrue();
        for (String amount : List.of("alinanPrimTutari", "iptalPrimTutari", "odenecekVergi",
                "vergiPrimTutari", "gecmisAyIadeTutari")) {
            assertThat(item.get(amount).isNumber()).as(amount + " is a JSON number").isTrue();
            assertThat(item.get(amount).isTextual()).as(amount + " is not a string").isFalse();
        }
    }

    // --- PUT --------------------------------------------------------------------------

    @Test
    @DisplayName("PUT leaves out ay, yil, ilKodu and ilceKodu")
    void toUpdateRequest_omitsPeriodAndLocationFields() throws Exception {
        List<DeclarationProcess> group = List.of(
                districtRow(1L, MovableType.MENKUL),
                districtRow(2L, MovableType.GAYRIMENKUL));

        SbmDeclarationRequest request = mapper.toUpdateRequest(group, COMPANY_CODE, false);

        assertThat(request.getAy()).isNull();
        assertThat(request.getYil()).isNull();
        assertThat(request.getIlKodu()).isNull();
        assertThat(request.getIlceKodu()).isNull();
        assertThat(request.getSigortaSirketKodu()).isEqualTo(COMPANY_CODE);
        assertThat(request.getYsvTutarList()).hasSize(2);

        String json = objectMapper.writeValueAsString(request);
        assertThat(json).doesNotContain("\"ay\"", "\"yil\"", "\"ilKodu\"", "\"ilceKodu\"");
        assertThat(json).contains("\"sigortaSirketKodu\":\"045\"", "\"ysvDosyaNo\"", "\"ysvTutarList\"");
    }

    @Test
    @DisplayName("PUT does not carry ilKodu/ilceKodu, so neither is validated")
    void toUpdateRequest_ignoresCityAndDistrict() {
        DeclarationProcess row = baseRow(1L, MovableType.MENKUL)
                .cityCode(null).districtCode(null).build();

        SbmDeclarationRequest request = mapper.toUpdateRequest(List.of(row), COMPANY_CODE, false);

        assertThat(request.getIlKodu()).isNull();
        assertThat(request.getIlceKodu()).isNull();
        assertThat(request.getYsvDosyaNo()).isEqualTo("YSV202513491");
    }

    @Test
    @DisplayName("cancel zeroes every amount but keeps the tax ratio")
    void toUpdateRequest_zeroAmounts_zeroesAllAmounts() {
        DeclarationProcess row = baseRow(1L, MovableType.MENKUL)
                .cityCode(1).districtCode(0)
                .prevMonthRefundAmount(new BigDecimal("125.50"))
                .build();

        SbmDeclarationRequest request = mapper.toUpdateRequest(List.of(row), COMPANY_CODE, true);

        SbmAmountItem item = request.getYsvTutarList().get(0);
        assertThat(item.getAlinanPrimTutari()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getIptalPrimTutari()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getOdenecekVergi()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getVergiPrimTutari()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getGecmisAyIadeTutari()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getVergiOrani()).isEqualTo(10);
    }

    // --- amounts ----------------------------------------------------------------------

    @Test
    @DisplayName("missing amounts are transferred as an explicit 0, not omitted")
    void nullAmounts_becomeZero() throws Exception {
        DeclarationProcess row = baseRow(1L, MovableType.GAYRIMENKUL)
                .cityCode(1).districtCode(0)
                .receivedPremiumAmount(null)
                .cancelledPremiumAmount(null)
                .taxAmount(null)
                .taxPremiumAmount(null)
                .taxRatio(null)
                .build();

        SbmDeclarationRequest request = mapper.toSendRequest(List.of(row), COMPANY_CODE);
        SbmAmountItem item = request.getYsvTutarList().get(0);

        assertThat(item.getAlinanPrimTutari()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getIptalPrimTutari()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getOdenecekVergi()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getVergiPrimTutari()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getVergiOrani()).isZero();

        String json = objectMapper.writeValueAsString(request);
        assertThat(json).contains("\"alinanPrimTutari\":0", "\"vergiOrani\":0");
    }

    @Test
    @DisplayName("zero amounts survive serialization (GAYRIMENKUL rows are usually all zero)")
    void zeroAmounts_areSerializedNotDropped() throws Exception {
        DeclarationProcess row = baseRow(1L, MovableType.GAYRIMENKUL)
                .cityCode(1).districtCode(0)
                .receivedPremiumAmount(BigDecimal.ZERO)
                .cancelledPremiumAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .taxPremiumAmount(BigDecimal.ZERO)
                .build();

        String json = objectMapper.writeValueAsString(mapper.toSendRequest(List.of(row), COMPANY_CODE));

        assertThat(json).contains("\"alinanPrimTutari\":0", "\"iptalPrimTutari\":0",
                "\"odenecekVergi\":0", "\"vergiPrimTutari\":0");
    }

    @Test
    @DisplayName("gecmisAyIadeTutari is only sent when the database holds a value")
    void prevMonthRefund_isOnlySentWhenPresent() throws Exception {
        DeclarationProcess without = cityLevelRow(1L, MovableType.MENKUL);
        DeclarationProcess with = baseRow(2L, MovableType.MENKUL)
                .cityCode(1).districtCode(0)
                .prevMonthRefundAmount(new BigDecimal("42.75"))
                .build();

        String withoutJson = objectMapper.writeValueAsString(
                mapper.toSendRequest(List.of(without), COMPANY_CODE));
        String withJson = objectMapper.writeValueAsString(
                mapper.toSendRequest(List.of(with), COMPANY_CODE));

        assertThat(withoutJson).doesNotContain("gecmisAyIadeTutari");
        assertThat(withJson).contains("\"gecmisAyIadeTutari\":42.75");
    }

    // --- group validation --------------------------------------------------------------

    @Test
    @DisplayName("a duplicated movable type is rejected with SBM's own RISK-HAVUZU-00005")
    void duplicateMovableType_throws() {
        List<DeclarationProcess> group = List.of(
                cityLevelRow(1L, MovableType.MENKUL),
                cityLevelRow(2L, MovableType.MENKUL));

        assertThatThrownBy(() -> mapper.toSendRequest(group, COMPANY_CODE))
                .isInstanceOf(SbmIntegrationException.class)
                .hasMessageContaining("mükerrer menkul tipi");
        assertSbmError(() -> mapper.toSendRequest(group, COMPANY_CODE),
                SbmErrorCode.RISK_HAVUZU_00005);
    }

    @Test
    void missingMovableType_throws() {
        DeclarationProcess row = baseRow(1L, null).cityCode(1).districtCode(0).build();

        assertSbmError(() -> mapper.toSendRequest(List.of(row), COMPANY_CODE),
                SbmErrorCode.CORE_01000);
    }

    @Test
    void emptyOrNullGroup_throws() {
        assertSbmError(() -> mapper.toSendRequest(List.of(), COMPANY_CODE), SbmErrorCode.CORE_01000);
        assertSbmError(() -> mapper.toSendRequest(null, COMPANY_CODE), SbmErrorCode.CORE_01000);
    }

    @Test
    void missingCompanyCode_throws() {
        List<DeclarationProcess> group = List.of(cityLevelRow(1L, MovableType.MENKUL));

        assertSbmError(() -> mapper.toSendRequest(group, null), SbmErrorCode.RISK_HAVUZU_00002);
        assertSbmError(() -> mapper.toSendRequest(group, "  "), SbmErrorCode.RISK_HAVUZU_00002);
    }

    @Test
    void missingFileNo_throws() {
        DeclarationProcess nullFileNo = baseRow(1L, MovableType.MENKUL)
                .cityCode(1).districtCode(0).sbmFileNo(null).build();
        DeclarationProcess blankFileNo = baseRow(2L, MovableType.MENKUL)
                .cityCode(1).districtCode(0).sbmFileNo("   ").build();

        assertSbmError(() -> mapper.toSendRequest(List.of(nullFileNo), COMPANY_CODE),
                SbmErrorCode.CORE_01000);
        assertSbmError(() -> mapper.toSendRequest(List.of(blankFileNo), COMPANY_CODE),
                SbmErrorCode.CORE_01000);
    }

    @Test
    @DisplayName("ysvDosyaNo is read back from the group, not from the grouping key")
    void fileNo_isTakenFromTheGroupRows() {
        DeclarationProcess withoutFileNo = baseRow(1L, MovableType.MENKUL)
                .cityCode(1).districtCode(0).sbmFileNo(null).build();
        DeclarationProcess withFileNo = baseRow(2L, MovableType.GAYRIMENKUL)
                .cityCode(1).districtCode(0).sbmFileNo(" YSV202513491 ").build();

        SbmDeclarationRequest request =
                mapper.toSendRequest(List.of(withoutFileNo, withFileNo), COMPANY_CODE);

        assertThat(request.getYsvDosyaNo()).isEqualTo("YSV202513491");
    }

    @Test
    @DisplayName("a group carrying several file numbers is sent with the first one")
    void mixedFileNumbers_useTheFirstOne() {
        DeclarationProcess first = baseRow(1L, MovableType.MENKUL)
                .cityCode(1).districtCode(0).sbmFileNo("YSV202513491").build();
        DeclarationProcess second = baseRow(2L, MovableType.GAYRIMENKUL)
                .cityCode(1).districtCode(0).sbmFileNo("YSV202599999").build();

        SbmDeclarationRequest request = mapper.toSendRequest(List.of(first, second), COMPANY_CODE);

        assertThat(request.getYsvDosyaNo()).isEqualTo("YSV202513491");
        assertThat(request.getYsvTutarList()).hasSize(2);
    }

    @Test
    @DisplayName("ysvDosyaNo longer than 36 characters is rejected with CORE-01008")
    void tooLongFileNo_throws() {
        DeclarationProcess row = baseRow(1L, MovableType.MENKUL)
                .cityCode(1).districtCode(0)
                .sbmFileNo("Y".repeat(37))
                .build();

        assertSbmError(() -> mapper.toSendRequest(List.of(row), COMPANY_CODE), SbmErrorCode.CORE_01008);
    }

    @Test
    void missingPaymentDate_throws() {
        DeclarationProcess row = baseRow(1L, MovableType.MENKUL)
                .cityCode(1).districtCode(0).paymentDate(null).build();

        assertSbmError(() -> mapper.toSendRequest(List.of(row), COMPANY_CODE), SbmErrorCode.CORE_01000);
    }

    // --- query -------------------------------------------------------------------------

    @Test
    void toQueryRequest_buildsBody() {
        SbmQueryRequest request = mapper.toQueryRequest(" YSV202513491 ", COMPANY_CODE);

        assertThat(request.getSigortaSirketKodu()).isEqualTo(COMPANY_CODE);
        assertThat(request.getYsvDosyaNo()).isEqualTo("YSV202513491");
    }

    @Test
    void toQueryRequest_rejectsMissingFileNo() {
        assertSbmError(() -> mapper.toQueryRequest(null, COMPANY_CODE), SbmErrorCode.CORE_01000);
        assertSbmError(() -> mapper.toQueryRequest("  ", COMPANY_CODE), SbmErrorCode.CORE_01000);
    }

    private static void assertSbmError(ThrowingCallable callable, SbmErrorCode expected) {
        Throwable thrown = catchThrowable(callable);
        assertThat(thrown).isInstanceOf(SbmIntegrationException.class);
        assertThat(((SbmIntegrationException) thrown).getErrorCode()).isEqualTo(expected.getCode());
    }
}
