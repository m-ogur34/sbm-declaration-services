package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One element of SBM's {@code ysvTutarList}. There is exactly one element per movable type
 * inside a declaration group (SBM rejects duplicates with RISK-HAVUZU-00005).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SbmAmountItem {

    /** Decimal. DB: {@code RECEIVED_PREMIUM_AMOUNT}. */
    private BigDecimal alinanPrimTutari;

    /** Decimal. DB: {@code CANCELLED_PREMIUM_AMOUNT}. */
    private BigDecimal iptalPrimTutari;

    /** String, "MENKUL" or "GAYRIMENKUL". DB: {@code MOVABLE_TYPE}. */
    private String menkulTipi;

    /** Decimal. DB: {@code TAX_AMOUNT}. */
    private BigDecimal odenecekVergi;

    /** Number. DB: {@code TAX_RATIO}. */
    private Integer vergiOrani;

    /** Decimal. DB: {@code TAX_PREMIUM_AMOUNT}. */
    private BigDecimal vergiPrimTutari;

    /**
     * Decimal. DB: {@code PREV_MONTH_REFUND_AMOUNT}. Belongs to the amount item, not to the
     * request root. Sent only when the database holds a value.
     */
    private BigDecimal gecmisAyIadeTutari;
}
