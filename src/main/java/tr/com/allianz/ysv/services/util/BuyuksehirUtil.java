package tr.com.allianz.ysv.services.util;

import java.util.Collections;
import java.util.Set;
import tr.com.allianz.ysv.services.enums.SbmErrorCode;
import tr.com.allianz.ysv.services.exception.SbmIntegrationException;

/**
 * Metropolitan municipality ("büyükşehir") rules of the SBM declaration.
 *
 * <ul>
 *   <li>Metropolitan city: {@code ilceKodu} must be omitted, otherwise SBM answers
 *       RISK-HAVUZU-00007.</li>
 *   <li>Any other city: {@code ilceKodu} is mandatory, otherwise SBM answers
 *       RISK-HAVUZU-00008.</li>
 * </ul>
 *
 * <p>In the OPUS extract a metropolitan row carries {@code DISTRICT_CODE = 0}, which is why
 * zero is treated exactly like a missing district code.</p>
 *
 * <p>TODO(confirm): the list below is the one handed over with the analysis. The OPUS
 * extract disagrees on two codes - it sends city 47 (Mardin) without a district and city 22
 * (Edirne) with districts - so the list should be reconciled with the business before the
 * first production run. See "Açık Konular" in README.md.</p>
 */
public final class BuyuksehirUtil {

    private static final Set<Integer> METROPOLITAN_CITY_CODES = Set.of(
            1, 6, 7, 9, 10, 16, 20, 21, 22, 25, 26, 27, 31, 33, 34, 35,
            38, 41, 42, 44, 45, 46, 48, 52, 54, 55, 59, 61, 63, 65);

    private BuyuksehirUtil() {
    }

    /**
     * @param cityCode SBM city code
     * @return {@code true} when the city is a metropolitan municipality
     */
    public static boolean isBuyuksehir(Integer cityCode) {
        return cityCode != null && METROPOLITAN_CITY_CODES.contains(cityCode);
    }

    /** @return the 30 metropolitan city codes, unmodifiable */
    public static Set<Integer> metropolitanCityCodes() {
        return Collections.unmodifiableSet(METROPOLITAN_CITY_CODES);
    }

    /**
     * Applies the metropolitan rule to a raw database district code without rejecting
     * anything. This is the value SBM identifies a declaration by, so it is also the value
     * rows are grouped on: a metropolitan city always collapses to a single city level
     * declaration, and a missing district (null or the 0 OPUS delivers) collapses to
     * {@code null} so that every unusable row of that city ends up in one group and fails
     * once, together.
     *
     * @param cityCode     SBM city code
     * @param districtCode district code as stored in the database (may be {@code null} or 0)
     * @return the district code SBM would see, possibly {@code null}
     */
    public static Integer groupingDistrictCode(Integer cityCode, Integer districtCode) {
        if (isBuyuksehir(cityCode)) {
            return null;
        }
        return (districtCode == null || districtCode == 0) ? null : districtCode;
    }

    /**
     * Applies the metropolitan rule to a raw database district code and rejects the rows SBM
     * would refuse.
     *
     * @param cityCode     SBM city code
     * @param districtCode district code as stored in the database (may be {@code null} or 0)
     * @return {@code null} for a metropolitan city, the district code otherwise
     * @throws SbmIntegrationException when the city code is missing (RISK-HAVUZU-00006), or
     *                                 when a non metropolitan city has no usable district
     *                                 code (RISK-HAVUZU-00008)
     */
    public static Integer resolveDistrictCode(Integer cityCode, Integer districtCode) {
        if (cityCode == null) {
            throw new SbmIntegrationException(SbmErrorCode.RISK_HAVUZU_00006.getCode(),
                    "İl kodu boş olduğu için beyanname gönderilemez.");
        }
        Integer resolved = groupingDistrictCode(cityCode, districtCode);
        if (resolved == null && !isBuyuksehir(cityCode)) {
            throw new SbmIntegrationException(SbmErrorCode.RISK_HAVUZU_00008.getCode(),
                    "İl " + cityCode + " büyükşehir değil, ilçe kodu zorunludur.");
        }
        return resolved;
    }
}
