package tr.com.allianz.ysv.services.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tr.com.allianz.ysv.services.entity.DeclarationLog;
import tr.com.allianz.ysv.services.enums.LogLevel;
import tr.com.allianz.ysv.services.enums.OperationType;
import tr.com.allianz.ysv.services.repository.DeclarationLogRepository;

@ExtendWith(MockitoExtension.class)
class DeclarationLogServiceTest {

    @Mock
    private DeclarationLogRepository declarationLogRepository;

    @InjectMocks
    private DeclarationLogService service;

    @Captor
    private ArgumentCaptor<List<DeclarationLog>> rowsCaptor;

    @Test
    @DisplayName("one evidence row is written per declaration line")
    void logCall_writesOneRowPerProcessId() {
        service.logCall(List.of(1L, 2L), OperationType.POST, LogLevel.INFO,
                "POST YSV1 başarılı", "{\"a\":1}", "{\"result\":true}");

        verify(declarationLogRepository).saveAll(rowsCaptor.capture());
        List<DeclarationLog> rows = rowsCaptor.getValue();

        assertThat(rows).hasSize(2)
                .extracting(DeclarationLog::getProcessId)
                .containsExactly(1L, 2L);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getOperationType()).isEqualTo(OperationType.POST);
            assertThat(row.getLogLevel()).isEqualTo(LogLevel.INFO);
            assertThat(row.getLogMessage()).isEqualTo("POST YSV1 başarılı");
            assertThat(row.getRequestPayload()).isEqualTo("{\"a\":1}");
            assertThat(row.getResponsePayload()).isEqualTo("{\"result\":true}");
            assertThat(row.getDateCreated()).isNotNull();
        });
    }

    @Test
    @DisplayName("a call that belongs to no row is still recorded, with a null PROCESS_ID")
    void logCall_writesASingleRowWhenThereAreNoProcessIds() {
        service.logCall(List.of(), OperationType.GET, LogLevel.ERROR, "GET başarısız", null, null);

        verify(declarationLogRepository).saveAll(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(1)
                .allSatisfy(row -> assertThat(row.getProcessId()).isNull());
    }

    @Test
    void logCall_acceptsNullProcessIds() {
        service.logCall(null, OperationType.GET, LogLevel.WARNING, "uyarı", null, null);

        verify(declarationLogRepository).saveAll(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("a failing audit insert never aborts the caller: the SBM call already happened")
    void logCall_swallowsPersistenceFailures() {
        when(declarationLogRepository.saveAll(anyList()))
                .thenThrow(new IllegalStateException("ORA-00942"));

        assertThatCode(() -> service.logCall(List.of(1L), OperationType.PUT, LogLevel.ERROR,
                "PUT başarısız", "{}", "{}")).doesNotThrowAnyException();
    }
}
