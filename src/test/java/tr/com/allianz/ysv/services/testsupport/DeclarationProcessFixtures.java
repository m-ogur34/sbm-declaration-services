package tr.com.allianz.ysv.services.testsupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import tr.com.allianz.ysv.services.entity.DeclarationProcess;
import tr.com.allianz.ysv.services.enums.MovableType;
import tr.com.allianz.ysv.services.enums.ProcessStatus;

/**
 * Builders for the declaration rows used across the unit tests. The default values mirror
 * the first group of the OPUS extract (city 1, January 2026).
 */
public final class DeclarationProcessFixtures {

    public static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 1, 20);
    public static final String FILE_NO = "YSV202513491";

    private DeclarationProcessFixtures() {
    }

    /** City level declaration: DISTRICT_CODE 0, exactly as OPUS delivers it. */
    public static DeclarationProcess cityLevelRow(Long id, MovableType movableType) {
        return baseRow(id, movableType).cityCode(1).districtCode(0).build();
    }

    /** District level declaration with a real district code. */
    public static DeclarationProcess districtRow(Long id, MovableType movableType) {
        return baseRow(id, movableType).cityCode(2).districtCode(1425).build();
    }

    public static DeclarationProcess.DeclarationProcessBuilder baseRow(Long id, MovableType movableType) {
        return DeclarationProcess.builder()
                .id(id)
                .declarationYear(2026)
                .declarationMonth(1)
                .companyCode("045")
                .paymentDate(PAYMENT_DATE)
                .sbmFileNo(FILE_NO)
                .movableType(movableType)
                .receivedPremiumAmount(new BigDecimal("7453723.22"))
                .cancelledPremiumAmount(new BigDecimal("15090.61"))
                .taxAmount(new BigDecimal("743863.26"))
                .taxRatio(10)
                .taxPremiumAmount(new BigDecimal("7438632.61"))
                .status(ProcessStatus.NEW);
    }
}
