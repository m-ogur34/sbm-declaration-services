package tr.com.allianz.ysv.services.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tr.com.allianz.ysv.services.dto.internal.SbmAmountItem;
import tr.com.allianz.ysv.services.dto.internal.SbmDeclarationRequest;
import tr.com.allianz.ysv.services.dto.internal.SbmQueryRequest;
import tr.com.allianz.ysv.services.entity.DeclarationProcess;
import tr.com.allianz.ysv.services.enums.MovableType;
import tr.com.allianz.ysv.services.enums.SbmErrorCode;
import tr.com.allianz.ysv.services.exception.SbmIntegrationException;
import tr.com.allianz.ysv.services.util.DistrictCodeResolver;

/**
 * Builds SBM request bodies out of a declaration group.
 *
 * <p>Only what would make the request meaningless is checked here - a missing file number,
 * payment date, city code or movable type, and a duplicated movable type - and it is
 * reported with SBM's own code so operators deal with one vocabulary. Everything else,
 * including whether a city / district combination is the one SBM expects, is left to SBM:
 * it answers RISK-HAVUZU-00007 / RISK-HAVUZU-00008 and the reason lands in
 * {@code ERROR_DETAILS}.</p>
 */
@Slf4j
@Component
public class SbmMapper {

    /** SBM limits {@code ysvDosyaNo} to 36 characters even though the column is wider. */
    static final int SBM_FILE_NO_MAX_LENGTH = 36;

    /**
     * Builds the POST body. {@code ay}, {@code yil}, {@code ilKodu} and {@code ilceKodu} are
     * sent; {@code ilceKodu} is left out when the database holds no district (null or 0).
     *
     * @param group all rows sharing one declaration group key, at least one element
     * @param companyCode SBM company code ("045")
     * @return the request body
     * @throws SbmIntegrationException when the group cannot produce a valid SBM request
     */
    public SbmDeclarationRequest toSendRequest(List<DeclarationProcess> group, String companyCode) {
        requireGroupAndCompanyCode(group, companyCode);
        String fileNo = resolveFileNo(group);
        DeclarationProcess head = requirePaymentDate(group.get(0), fileNo);
        if (head.getCityCode() == null) {
            throw new SbmIntegrationException(SbmErrorCode.CORE_01000.getCode(),
                    "ilKodu boş olamaz. Dosya no: " + fileNo);
        }
        return baseRequest(head, companyCode, group, fileNo, false)
                .ay(head.getDeclarationMonth())
                .yil(head.getDeclarationYear())
                .ilKodu(head.getCityCode())
                .ilceKodu(DistrictCodeResolver.resolve(head.getDistrictCode()))
                .build();
    }

    /**
     * Builds the PUT body. Period and location fields are left {@code null} so that
     * {@code @JsonInclude(NON_NULL)} keeps them out of the payload, exactly as the SBM
     * document requires.
     *
     * @param group all rows sharing one declaration group key, at least one element
     * @param companyCode SBM company code ("045")
     * @param zeroAmounts {@code true} for the cancel flow, which zeroes every amount
     * @return the request body
     * @throws SbmIntegrationException when the group cannot produce a valid SBM request
     */
    public SbmDeclarationRequest toUpdateRequest(List<DeclarationProcess> group,
                                                 String companyCode,
                                                 boolean zeroAmounts) {
        requireGroupAndCompanyCode(group, companyCode);
        String fileNo = resolveFileNo(group);
        DeclarationProcess head = requirePaymentDate(group.get(0), fileNo);
        // No city or district check: PUT does not carry those fields at all.
        return baseRequest(head, companyCode, group, fileNo, zeroAmounts).build();
    }

    /**
     * @param ysvDosyaNo SBM file number to look up
     * @param companyCode SBM company code ("045")
     * @return the query body
     */
    public SbmQueryRequest toQueryRequest(String ysvDosyaNo, String companyCode) {
        if (ysvDosyaNo == null || ysvDosyaNo.isBlank()) {
            throw new SbmIntegrationException(SbmErrorCode.CORE_01000.getCode(),
                    "Sorgu için ysvDosyaNo zorunludur.");
        }
        return SbmQueryRequest.builder()
                .sigortaSirketKodu(companyCode)
                .ysvDosyaNo(ysvDosyaNo.trim())
                .build();
    }

    private SbmDeclarationRequest.SbmDeclarationRequestBuilder baseRequest(DeclarationProcess head,
                                                                          String companyCode,
                                                                          List<DeclarationProcess> group,
                                                                          String fileNo,
                                                                          boolean zeroAmounts) {
        return SbmDeclarationRequest.builder()
                .sigortaSirketKodu(companyCode)
                .sonOdemeTarihi(head.getPaymentDate())
                .ysvDosyaNo(fileNo)
                .ysvTutarList(toAmountList(group, zeroAmounts));
    }

    private void requireGroupAndCompanyCode(List<DeclarationProcess> group, String companyCode) {
        if (group == null || group.isEmpty()) {
            throw new SbmIntegrationException(SbmErrorCode.CORE_01000.getCode(),
                    "Gönderilecek beyanname satırı bulunamadı.");
        }
        if (companyCode == null || companyCode.isBlank()) {
            throw new SbmIntegrationException(SbmErrorCode.RISK_HAVUZU_00002.getCode(),
                    "Sigorta şirket kodu tanımlı değil.");
        }
    }

    /**
     * @return {@code head} unchanged, once it is known to carry a payment date
     */
    private DeclarationProcess requirePaymentDate(DeclarationProcess head, String fileNo) {
        if (head.getPaymentDate() == null) {
            throw new SbmIntegrationException(SbmErrorCode.CORE_01000.getCode(),
                    "sonOdemeTarihi boş olamaz. Dosya no: " + fileNo);
        }
        return head;
    }

    /**
     * {@code ysvDosyaNo} is not part of the grouping key - SBM identifies a declaration by
     * İl-İlçe-Yıl-Ay - so it is read back from the rows of the group. One declaration is
     * expected to carry one file number; a group holding several is a data problem worth an
     * operator's attention, but not a reason to stop the transfer, so the first one is used
     * and the rest are reported in the application log.
     *
     * @param group rows of one declaration group
     * @return the file number to send
     * @throws SbmIntegrationException when no usable file number exists, or it is too long
     */
    private String resolveFileNo(List<DeclarationProcess> group) {
        Set<String> fileNumbers = new LinkedHashSet<>();
        for (DeclarationProcess process : group) {
            String candidate = process.getSbmFileNo();
            if (candidate != null && !candidate.isBlank()) {
                fileNumbers.add(candidate.trim());
            }
        }
        if (fileNumbers.isEmpty()) {
            throw new SbmIntegrationException(SbmErrorCode.CORE_01000.getCode(),
                    "ysvDosyaNo boş olamaz.");
        }
        String fileNo = fileNumbers.iterator().next();
        if (fileNumbers.size() > 1) {
            log.warn("Declaration group {}/{} il={} ilce={} carries {} different ysvDosyaNo values {}; "
                            + "the first one is sent. Check the OPUS extract.",
                    group.get(0).getDeclarationYear(), group.get(0).getDeclarationMonth(),
                    group.get(0).getCityCode(), group.get(0).getDistrictCode(),
                    fileNumbers.size(), fileNumbers);
        }
        if (fileNo.length() > SBM_FILE_NO_MAX_LENGTH) {
            throw new SbmIntegrationException(SbmErrorCode.CORE_01008.getCode(),
                    "ysvDosyaNo en fazla " + SBM_FILE_NO_MAX_LENGTH + " karakter olabilir: " + fileNo);
        }
        return fileNo;
    }

    private List<SbmAmountItem> toAmountList(List<DeclarationProcess> group, boolean zeroAmounts) {
        Set<MovableType> seen = EnumSet.noneOf(MovableType.class);
        List<SbmAmountItem> items = new ArrayList<>(group.size());
        for (DeclarationProcess process : group) {
            MovableType movableType = process.getMovableType();
            if (movableType == null) {
                throw new SbmIntegrationException(SbmErrorCode.CORE_01000.getCode(),
                        "menkulTipi boş olamaz. Dosya no: " + process.getSbmFileNo());
            }
            if (!seen.add(movableType)) {
                throw new SbmIntegrationException(SbmErrorCode.RISK_HAVUZU_00005.getCode(),
                        "Aynı beyannamede mükerrer menkul tipi var: " + movableType.getSbmValue()
                                + ". Dosya no: " + process.getSbmFileNo());
            }
            items.add(toAmountItem(process, movableType, zeroAmounts));
        }
        return items;
    }

    private SbmAmountItem toAmountItem(DeclarationProcess process,
                                       MovableType movableType,
                                       boolean zeroAmounts) {
        return SbmAmountItem.builder()
                .menkulTipi(movableType.getSbmValue())
                .alinanPrimTutari(amount(process.getReceivedPremiumAmount(), zeroAmounts))
                .iptalPrimTutari(amount(process.getCancelledPremiumAmount(), zeroAmounts))
                .odenecekVergi(amount(process.getTaxAmount(), zeroAmounts))
                .vergiPrimTutari(amount(process.getTaxPremiumAmount(), zeroAmounts))
                .vergiOrani(process.getTaxRatio() == null ? 0 : process.getTaxRatio())
                .gecmisAyIadeTutari(refundAmount(process.getPrevMonthRefundAmount(), zeroAmounts))
                .build();
    }

    /** SBM requires every amount, so a missing value is transferred as an explicit zero. */
    private static BigDecimal amount(BigDecimal value, boolean zeroAmounts) {
        if (zeroAmounts || value == null) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    /**
     * {@code gecmisAyIadeTutari} is not part of the documented SBM field list, so it is only
     * sent when the database actually holds a value.
     */
    private static BigDecimal refundAmount(BigDecimal value, boolean zeroAmounts) {
        if (value == null) {
            return null;
        }
        return zeroAmounts ? BigDecimal.ZERO : value;
    }
}
