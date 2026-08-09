package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response of the SBM {@code ysv-beyanname} POST/PUT calls, both for the success and for the
 * 4xx/5xx bodies.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SbmDeclarationResponse {

    private Boolean result;

    private Integer status;

    private String ysvDosyaNo;

    private SbmError error;
}
