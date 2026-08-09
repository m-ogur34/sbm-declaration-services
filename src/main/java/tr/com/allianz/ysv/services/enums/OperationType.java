package tr.com.allianz.ysv.services.enums;

import lombok.Getter;

/**
 * Operation carried out against SBM. The name is persisted in
 * {@code ALZ_SBM_DECL_LOG.OPERATION_TYPE} and is also used to pick the
 * {@code functionName} sent to alz-token-management.
 */
@Getter
public enum OperationType {

    POST("ysv-beyanname-gonder"),
    PUT("ysv-beyanname-guncelle"),
    GET("ysv-beyanname-sorgu");

    private final String tokenFunctionName;

    OperationType(String tokenFunctionName) {
        this.tokenFunctionName = tokenFunctionName;
    }
}
