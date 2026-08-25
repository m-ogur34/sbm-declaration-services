package tr.com.allianz.ysv.services.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProcessView(Long id,
                          Integer declarationYear,
                          Integer declarationMonth,
                          Integer cityCode,
                          Integer districtCode,
                          String sbmFileNo,
                          String movableType,
                          String status,
                          LocalDate paymentDate,
                          BigDecimal receivedPremiumAmount,
                          BigDecimal cancelledPremiumAmount,
                          BigDecimal taxAmount,
                          BigDecimal taxPremiumAmount,
                          Integer taxRatio,
                          LocalDateTime dateSent,
                          String errorDetails) {
}
