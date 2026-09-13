package tr.com.allianz.ysv.services.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OperationTypeTest {

    @Test
    void enumSurfaceIsStable() {
        assertThat(OperationType.values()).hasSize(3);
        assertThat(OperationType.valueOf("PUT")).isSameAs(OperationType.PUT);
        assertThat(OperationType.valueOf("POST")).isSameAs(OperationType.POST);
        assertThat(OperationType.valueOf("GET")).isSameAs(OperationType.GET);
    }

    @Test
    void namesAreThePersistedOperationTypeValues() {
        assertThat(OperationType.POST.name()).isEqualTo("POST");
        assertThat(OperationType.PUT.name()).isEqualTo("PUT");
        assertThat(OperationType.GET.name()).isEqualTo("GET");
    }
}
