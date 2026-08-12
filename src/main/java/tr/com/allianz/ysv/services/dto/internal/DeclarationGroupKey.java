package tr.com.allianz.ysv.services.dto.internal;

import tr.com.allianz.ysv.services.entity.DeclarationProcess;
import tr.com.allianz.ysv.services.util.DistrictCodeResolver;

/**
 * Grouping key of a single SBM request.
 *
 * <p>SBM identifies a declaration by <b>İl - İlçe - Yıl - Ay</b> and rejects a second one for
 * the same combination with RISK-HAVUZU-00004, so those four values - and only those - form
 * the key. {@code ysvDosyaNo} is deliberately <b>not</b> part of it: the file number is
 * chosen freely by the insurer, so keying on it would split one legal declaration into
 * several requests. It is read back from the rows of the group instead.</p>
 *
 * <p>{@code districtCode} holds the value SBM would see, not the raw column: it goes through
 * the same {@link DistrictCodeResolver} normalization as the payload, so the 0 OPUS delivers
 * for a city level declaration and a {@code null} column end up in one and the same group.</p>
 *
 * @param declarationYear  SBM {@code yil}
 * @param declarationMonth SBM {@code ay}
 * @param cityCode         SBM {@code ilKodu}
 * @param districtCode     SBM {@code ilceKodu}, {@code null} when no district is sent
 */
public record DeclarationGroupKey(Integer declarationYear,
                                  Integer declarationMonth,
                                  Integer cityCode,
                                  Integer districtCode) {

    public static DeclarationGroupKey of(DeclarationProcess process) {
        return new DeclarationGroupKey(process.getDeclarationYear(),
                process.getDeclarationMonth(),
                process.getCityCode(),
                DistrictCodeResolver.resolve(process.getDistrictCode()));
    }
}
