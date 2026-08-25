package tr.com.allianz.ysv.services.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

@Schema(description = "Beyanname toplu işlem filtresi")
public record DeclarationFilterRequest(

        @Schema(example = "2026")
        @Min(2000) @Max(2099)
        Integer year,

        @Schema(example = "1")
        @Min(1) @Max(12)
        Integer month,

        @Schema(example = "34")
        @Min(1) @Max(81)
        Integer cityCode,

        @Schema(description = "Belirtilirse diğer filtreler dikkate alınmaz")
        List<Long> processIds) {

    /** @return {@code true} when the caller pinned an explicit set of process ids */
    public boolean hasProcessIds() {
        return processIds != null && !processIds.isEmpty();
    }
}
