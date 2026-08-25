package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SbmDeclarationRequest {

    /** Number, POST only. DB: {@code DECLARATION_MONTH}. */
    private Integer ay;

    /** Number, POST only. DB: {@code CITY_CODE}. */
    private Integer ilKodu;

    /** Number, POST only. DB: {@code DISTRICT_CODE}; omitted when the column is null or 0. */
    private Integer ilceKodu;

    /** Number, POST only. DB: {@code DECLARATION_YEAR}. */
    private Integer yil;

    /** String, max 3. Always "045" for Allianz. */
    private String sigortaSirketKodu;

    /** LocalDate serialized as {@code yyyy-MM-dd}. DB: {@code PAYMENT_DATE}. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate sonOdemeTarihi;

    /** String, max 36. DB: {@code SBM_FILE_NO}. */
    private String ysvDosyaNo;

    /** One element per movable type; see {@link SbmAmountItem}. */
    private List<SbmAmountItem> ysvTutarList;
}
