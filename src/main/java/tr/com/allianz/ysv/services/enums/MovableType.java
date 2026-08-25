package tr.com.allianz.ysv.services.enums;

import lombok.Getter;

/**
 * SBM {@code menkulTipi}.
 *
 * <p>The REST contract only knows the string form, so that is the only form this enum
 * carries. The legacy SOAP integration used to put the numeric OPUS codes (1 = MENKUL,
 * 2 = GAYRIMENKUL) on the wire; that translation now happens in the OPUS load script, which
 * writes {@code ALZ_SBM_DECL_PROCESS.MOVABLE_TYPE} as {@code 'MENKUL'} / {@code 'GAYRIMENKUL'}
 * (the column has a CHECK constraint for exactly those two values). Nothing numeric can
 * therefore reach SBM.</p>
 */
@Getter
public enum MovableType {

    MENKUL("MENKUL"),
    GAYRIMENKUL("GAYRIMENKUL");

    /** The value SBM's REST contract expects. */
    private final String sbmValue;

    MovableType(String sbmValue) {
        this.sbmValue = sbmValue;
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
