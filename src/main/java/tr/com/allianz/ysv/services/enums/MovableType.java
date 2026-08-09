package tr.com.allianz.ysv.services.enums;

import lombok.Getter;

/**
 * SBM {@code menkulTipi}.
 *
 * <p>{@code code} is the OPUS/Excel numeric representation (1/2) that the legacy SOAP
 * integration used to put on the wire. The REST contract expects the string value, so
 * {@code sbmValue} is the only thing that ever reaches SBM: no SBM DTO has a numeric
 * {@code menkulTipi} field. {@code code} exists purely to interpret OPUS data on the way
 * into {@code ALZ_SBM_DECL_PROCESS.MOVABLE_TYPE}.</p>
 */
@Getter
public enum MovableType {

    MENKUL(1, "MENKUL"),
    GAYRIMENKUL(2, "GAYRIMENKUL");

    /** OPUS/Excel numeric code. Never sent to SBM. */
    private final int code;

    /** The value SBM's REST contract expects: "MENKUL" / "GAYRIMENKUL". */
    private final String sbmValue;

    MovableType(int code, String sbmValue) {
        this.code = code;
        this.sbmValue = sbmValue;
    }

    /**
     * @param code OPUS numeric code (1 = MENKUL, 2 = GAYRIMENKUL)
     * @return the matching type, never {@code null}
     * @throws IllegalArgumentException when the code is unknown
     */
    public static MovableType fromCode(Integer code) {
        if (code != null) {
            for (MovableType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Gecersiz menkul tipi kodu: " + code);
    }

    /**
     * @param sbmValue SBM string value ("MENKUL" / "GAYRIMENKUL")
     * @return the matching type, never {@code null}
     * @throws IllegalArgumentException when the value is unknown
     */
    public static MovableType fromSbmValue(String sbmValue) {
        if (sbmValue != null) {
            for (MovableType type : values()) {
                if (type.sbmValue.equalsIgnoreCase(sbmValue.trim())) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Gecersiz menkul tipi degeri: " + sbmValue);
    }
}
