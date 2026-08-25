package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response of the SBM {@code ysv-beyanname/sorgu} call.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SbmQueryResponse {

    private Boolean result;

    private Integer status;

    private Integer ay;

    private Integer yil;

    private Integer ilKodu;

    private Integer ilceKodu;

    private String sigortaSirketKodu;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate sonOdemeTarihi;

    private String ysvDosyaNo;

    private List<SbmAmountItem> ysvTutarList;

    private SbmError error;
}
