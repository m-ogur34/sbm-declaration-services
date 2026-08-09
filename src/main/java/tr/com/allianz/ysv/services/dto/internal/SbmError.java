package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SBM's {@code error} object. The timestamp is kept as the raw string SBM sent so that an
 * unexpected format can never make the whole response unparsable.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SbmError {

    private String timestamp;

    private List<SbmErrorReason> reasons;
}
