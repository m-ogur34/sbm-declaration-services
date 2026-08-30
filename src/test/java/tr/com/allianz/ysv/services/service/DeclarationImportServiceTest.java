package tr.com.allianz.ysv.services.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tr.com.allianz.ysv.services.config.SbmProperties;
import tr.com.allianz.ysv.services.dto.response.ExcelRowError;
import tr.com.allianz.ysv.services.dto.response.ImportResultResponse;
import tr.com.allianz.ysv.services.entity.DeclarationProcess;
import tr.com.allianz.ysv.services.enums.MovableType;
import tr.com.allianz.ysv.services.enums.ProcessStatus;
import tr.com.allianz.ysv.services.repository.DeclarationProcessRepository;
import tr.com.allianz.ysv.services.service.ExcelDeclarationParser.ParsedRow;
import tr.com.allianz.ysv.services.service.ExcelDeclarationParser.ParsedSheet;

@ExtendWith(MockitoExtension.class)
class DeclarationImportServiceTest {

    private static final String USER = "WDA2422";

    @Mock
    private ExcelDeclarationParser parser;
    @Mock
    private DeclarationProcessRepository repository;

    private DeclarationImportService service;

    @BeforeEach
    void setUp() {
        SbmProperties sbmProperties = new SbmProperties();
        sbmProperties.setCompanyCode("045");
        service = new DeclarationImportService(parser, repository, sbmProperties);
    }

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "beyanname.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());
    }

    private static ParsedRow row(int rowNumber, String fileNo, MovableType type, int month) {
        return new ParsedRow(rowNumber, month, 34, 0, 2026, fileNo, LocalDate.of(2026, month, 28),
                type, new BigDecimal("1.00"), new BigDecimal("1.00"), new BigDecimal("1.00"),
                10, new BigDecimal("1.00"), null);
    }

    @Test
    @DisplayName("aynı beyannamede MENKUL + GAYRIMENKUL: iki satır da NEW olarak yazılır")
    void importFile_insertsValidRows_companyCodeForced() {
        when(parser.parse(any())).thenReturn(new ParsedSheet(
                List.of(row(2, "YSV-1", MovableType.MENKUL, 8),
                        row(3, "YSV-1", MovableType.GAYRIMENKUL, 8)),
                List.of()));
        when(repository.existsBySbmFileNo(anyString())).thenReturn(false);

        ImportResultResponse result = service.importFile(file(), USER);

        assertThat(result.inserted()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.sourceFileName()).isEqualTo("beyanname.xlsx");

        ArgumentCaptor<List<DeclarationProcess>> captor = ArgumentCaptor.captor();
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).allSatisfy(p -> {
            assertThat(p.getCompanyCode()).isEqualTo("045");
            assertThat(p.getStatus()).isEqualTo(ProcessStatus.NEW);
            assertThat(p.getCreatedByUser()).isEqualTo(USER);
            assertThat(p.getSourceFileName()).isEqualTo("beyanname.xlsx");
        });
    }

    @Test
    @DisplayName("DB'de zaten olan ysvDosyaNo reddedilir, diğerleri yazılır")
    void importFile_duplicateFileNo_isReportedNotInserted() {
        when(parser.parse(any())).thenReturn(new ParsedSheet(
                List.of(row(2, "YSV-DUP", MovableType.MENKUL, 8),
                        row(3, "YSV-NEW", MovableType.MENKUL, 8)),
                List.of()));
        when(repository.existsBySbmFileNo("YSV-DUP")).thenReturn(true);
        when(repository.existsBySbmFileNo("YSV-NEW")).thenReturn(false);

        ImportResultResponse result = service.importFile(file(), USER);

        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.errors()).singleElement().satisfies(e -> {
            assertThat(e.code()).isEqualTo("ALZ-EXCEL-DUPLICATE");
            assertThat(e.ysvDosyaNo()).isEqualTo("YSV-DUP");
        });
    }

    @Test
    @DisplayName("parser'dan gelen satır hataları sonuç raporuna eklenir")
    void importFile_carriesParserRowErrors() {
        when(parser.parse(any())).thenReturn(new ParsedSheet(
                List.of(row(2, "YSV-OK", MovableType.MENKUL, 8)),
                List.of(new ExcelRowError(3, "YSV-BAD", "ALZ-EXCEL-FIELD", "ay boş olamaz"))));
        when(repository.existsBySbmFileNo(anyString())).thenReturn(false);

        ImportResultResponse result = service.importFile(file(), USER);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    @DisplayName("bir dosya birden fazla (yıl, ay) içeremez -> tüm dosya reddedilir")
    void importFile_multiplePeriods_rejectsWholeFile() {
        when(parser.parse(any())).thenReturn(new ParsedSheet(
                List.of(row(2, "YSV-1", MovableType.MENKUL, 7),
                        row(3, "YSV-2", MovableType.MENKUL, 8)),
                List.of()));

        assertThatThrownBy(() -> service.importFile(file(), USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("birden fazla dönem");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void importFile_emptySheet_insertsNothing() {
        when(parser.parse(any())).thenReturn(new ParsedSheet(List.of(), List.of()));

        ImportResultResponse result = service.importFile(file(), USER);

        assertThat(result.inserted()).isZero();
        assertThat(result.totalRows()).isZero();
        verify(repository).saveAll(List.of());
    }
}
