package tr.com.allianz.ysv.services.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProcessStatusTest {

    @Test
    void sendOperationOnlyPicksUpNewAndErrorRows() {
        assertThat(ProcessStatus.NEW.isSendable()).isTrue();
        assertThat(ProcessStatus.ERROR.isSendable()).isTrue();
        assertThat(ProcessStatus.PROCESSING.isSendable()).isFalse();
        assertThat(ProcessStatus.SENT.isSendable()).isFalse();
        assertThat(ProcessStatus.COMPLETED.isSendable()).isFalse();
        assertThat(ProcessStatus.SENDABLE).containsExactlyInAnyOrder(ProcessStatus.NEW, ProcessStatus.ERROR);
    }

    @Test
    void updateOperationOnlyPicksUpSentAndCompletedRows() {
        assertThat(ProcessStatus.SENT.isUpdatable()).isTrue();
        assertThat(ProcessStatus.COMPLETED.isUpdatable()).isTrue();
        assertThat(ProcessStatus.NEW.isUpdatable()).isFalse();
        assertThat(ProcessStatus.ERROR.isUpdatable()).isFalse();
        assertThat(ProcessStatus.PROCESSING.isUpdatable()).isFalse();
        assertThat(ProcessStatus.UPDATABLE).containsExactlyInAnyOrder(ProcessStatus.SENT, ProcessStatus.COMPLETED);
    }

    @Test
    void enumSurfaceIsStable() {
        assertThat(ProcessStatus.values()).hasSize(5);
        assertThat(ProcessStatus.valueOf("PROCESSING")).isSameAs(ProcessStatus.PROCESSING);
    }
}
