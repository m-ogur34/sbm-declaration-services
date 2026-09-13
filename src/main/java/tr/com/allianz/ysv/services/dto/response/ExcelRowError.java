package tr.com.allianz.ysv.services.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 1. AŞAMA — Excel yüklemede reddedilen tek bir satırın hata kaydı.
 *
 * @param rowNumber Excel'deki satır numarası (1 tabanlı, başlık satırı 1)
 * @param ysvDosyaNo satırdaki dosya numarası (okunabildiyse), aksi halde {@code null}
 * @param code       hata kodu (ör. {@code ALZ-EXCEL-DUPLICATE}, {@code ALZ-EXCEL-FIELD})
 * @param message    Türkçe açıklama
 */
@Schema(description = "Excel yüklemede reddedilen satır")
public record ExcelRowError(int rowNumber, String ysvDosyaNo, String code, String message) {
}
