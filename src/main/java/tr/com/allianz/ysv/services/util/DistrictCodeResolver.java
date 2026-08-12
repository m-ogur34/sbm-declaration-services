package tr.com.allianz.ysv.services.util;

/**
 * The single rule this application applies to {@code ilceKodu}.
 *
 * <p>OPUS delivers a metropolitan (büyükşehir) declaration with {@code DISTRICT_CODE = 0},
 * and SBM refuses a district for those cities (RISK-HAVUZU-00007). So a missing district -
 * {@code null} or the 0 OPUS uses for it - simply means "do not send the field", and
 * {@code @JsonInclude(NON_NULL)} keeps it out of the payload: {@code "ilceKodu": 0} is never
 * put on the wire. Every other value is forwarded exactly as the database holds it.</p>
 *
 * <p>There is deliberately <b>no</b> metropolitan city list and no "a non metropolitan city
 * must have a district" validation here. The source spreadsheet is maintained by the
 * business unit on a city / district basis, and SBM already reports a wrong combination with
 * RISK-HAVUZU-00007 or RISK-HAVUZU-00008, which lands in {@code ERROR_DETAILS}. Duplicating
 * that judgement locally would only block declarations the business considers correct.</p>
 */
public final class DistrictCodeResolver {

    private DistrictCodeResolver() {
    }

    /**
     * @param districtCode district code as stored in the database, may be {@code null} or 0
     * @return the value to put in {@code ilceKodu}, or {@code null} to leave the field out
     */
    public static Integer resolve(Integer districtCode) {
        if (districtCode == null || districtCode == 0) {
            return null;
        }
        return districtCode;
    }
}
