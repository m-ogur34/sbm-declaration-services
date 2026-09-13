package tr.com.allianz.ysv.services.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 1. AŞAMA — Excel yükleme sonucu. Geçerli satırlar {@code ALZ_SBM_DECL_PROCESS}'e
 * {@code STATUS=NEW} ile yazılır; hatalı satırlar {@link #errors} içinde raporlanır
 * (tüm dosya reddedilmez — sadece "tek ay" ihlalinde HTTP 400).
 *
 * @param sourceFileName yüklenen dosyanın adı
 * @param totalRows      başlık hariç okunan veri satırı sayısı
 * @param inserted       DB'ye yazılan satır sayısı
 * @param failed         reddedilen satır sayısı ({@code errors.size()})
 * @param errors         reddedilen satırların detayları
 */
@Schema(description = "Excel yükleme sonucu")
public record ImportResultResponse(String sourceFileName,
                                   int totalRows,
                                   int inserted,
                                   int failed,
                                   List<ExcelRowError> errors) {

    public static ImportResultResponse of(String sourceFileName, int totalRows, int inserted,
                                          List<ExcelRowError> errors) {
        List<ExcelRowError> safe = errors == null ? List.of() : List.copyOf(errors);
        return new ImportResultResponse(sourceFileName, totalRows, inserted, safe.size(), safe);
    }
}
