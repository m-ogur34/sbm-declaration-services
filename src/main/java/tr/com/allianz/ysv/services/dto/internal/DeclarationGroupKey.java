package tr.com.allianz.ysv.services.dto.internal;

import tr.com.allianz.ysv.services.entity.DeclarationProcess;
import tr.com.allianz.ysv.services.util.BuyuksehirUtil;

/**
 * Grouping key of a single SBM request.
 *
 * <p>SBM identifies a declaration by <b>İl - İlçe - Yıl - Ay</b> and rejects a second one for
 * the same combination with RISK-HAVUZU-00004, so those four values - and only those - form
 * the key. {@code ysvDosyaNo} is deliberately <b>not</b> part of it: the file number is
 * chosen freely by the insurer, so keying on it would split one legal declaration into
 * several requests. It is read back from the rows of the group instead.</p>
 *
 * <p>{@code districtCode} holds the value SBM would see, not the raw column: metropolitan
 * cities collapse to {@code null} (RISK-HAVUZU-00007) and so does a missing district, which
 * keeps every row of one declaration in one group.</p>
 *
 * @param declarationYear  SBM {@code yil}
 * @param declarationMonth SBM {@code ay}
 * @param cityCode         SBM {@code ilKodu}
 * @param districtCode     SBM {@code ilceKodu}, {@code null} for metropolitan cities
 */
public record DeclarationGroupKey(Integer declarationYear,
                                  Integer declarationMonth,
                                  Integer cityCode,
                                  Integer districtCode) {

    public static DeclarationGroupKey of(DeclarationProcess process) {
        return new DeclarationGroupKey(process.getDeclarationYear(),
                process.getDeclarationMonth(),
                process.getCityCode(),
                BuyuksehirUtil.groupingDistrictCode(process.getCityCode(), process.getDistrictCode()));
    }
}
