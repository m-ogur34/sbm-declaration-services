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

    /**
     * Excel'den gelen menkul tipini çözer. İş biriminden metin ({@code MENKUL} /
     * {@code GAYRIMENKUL}) istenmekle birlikte, kaynak OPUS ekstresi hâlâ sayısal
     * ({@code 1} / {@code 2}) verebildiği için ikisi de kabul edilir. POI sayısal
     * hücreyi {@code "1.0"} gibi de verebilir; o da tolere edilir.
     *
     * @param raw Excel hücresinin metin hâli, {@code null}/boş olabilir
     * @return eşleşen tip
     * @throws IllegalArgumentException değer tanınamazsa
     */
    public static MovableType fromExcel(String raw) {
        if (raw != null) {
            String value = raw.trim();
            if (value.endsWith(".0")) {
                value = value.substring(0, value.length() - 2);
            }
            if ("1".equals(value)) {
                return MENKUL;
            }
            if ("2".equals(value)) {
                return GAYRIMENKUL;
            }
            for (MovableType type : values()) {
                if (type.sbmValue.equalsIgnoreCase(value)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Geçersiz menkulTipi değeri: " + raw
                + " (beklenen: 1/2 veya MENKUL/GAYRIMENKUL)");
    }
}
