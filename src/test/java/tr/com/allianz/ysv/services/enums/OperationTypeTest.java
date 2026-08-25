package tr.com.allianz.ysv.services.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OperationTypeTest {

    @Test
    void eachOperationCarriesItsTokenFunctionName() {
        assertThat(OperationType.POST.getTokenFunctionName()).isEqualTo("ysv-beyanname-gonder");
        assertThat(OperationType.PUT.getTokenFunctionName()).isEqualTo("ysv-beyanname-guncelle");
        assertThat(OperationType.GET.getTokenFunctionName()).isEqualTo("ysv-beyanname-sorgu");
    }

    @Test
    void enumSurfaceIsStable() {
        assertThat(OperationType.values()).hasSize(3);
        assertThat(OperationType.valueOf("PUT")).isSameAs(OperationType.PUT);
    }
}
