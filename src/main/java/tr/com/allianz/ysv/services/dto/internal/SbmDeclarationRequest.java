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

/**
 * Body of the SBM {@code ysv-beyanname} POST (send) and PUT (update) calls.
 *
 * <p>{@code ay}, {@code yil}, {@code ilKodu} and {@code ilceKodu} are POST-only fields. The
 * update mapping leaves them {@code null} and {@code NON_NULL} inclusion keeps them out of
 * the PUT body. {@code ilceKodu} is also {@code null} for metropolitan municipalities on
 * POST (RISK-HAVUZU-00007).</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SbmDeclarationRequest {

    private Integer ay;

    private Integer ilKodu;

    private Integer ilceKodu;

    private Integer yil;

    private String sigortaSirketKodu;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate sonOdemeTarihi;

    private String ysvDosyaNo;

    private List<SbmAmountItem> ysvTutarList;
}
