package tr.com.allianz.ysv.services.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tr.com.allianz.ysv.services.controller.DeclarationController;
import tr.com.allianz.ysv.services.dto.request.DeclarationFilterRequest;
import tr.com.allianz.ysv.services.enums.SbmErrorCode;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/declarations/send");
    }

    @Test
    @DisplayName("an SBM failure keeps SBM's code and adds its Turkish description")
    void handleSbmIntegration_returns502WithSbmCode() {
        ResponseEntity<ErrorResponse> response = handler.handleSbmIntegration(
                new SbmIntegrationException(SbmErrorCode.RISK_HAVUZU_00007.getCode(),
                        "Büyükşehirde ilçe gönderilemez."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RISK-HAVUZU-00007");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/declarations/send");
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().details())
                .containsExactly(SbmErrorCode.RISK_HAVUZU_00007.getDescription());
    }

    @Test
    void handleToken_returns503() {
        ResponseEntity<ErrorResponse> response =
                handler.handleToken(new TokenException("Token servisine erişilemedi"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(SbmErrorCode.SEC_00001.getCode());
        assertThat(response.getBody().details()).containsExactly("Token servisine erişilemedi");
    }

    @Test
    void handleValidation_listsEveryFieldError() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "declarationFilterRequest");
        bindingResult.addError(new FieldError("declarationFilterRequest", "month",
                "en fazla 12 olmalıdır"));
        MethodParameter parameter = new MethodParameter(
                DeclarationController.class.getDeclaredMethod("send",
                        DeclarationFilterRequest.class, String.class), 0);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(
                new MethodArgumentNotValidException(parameter, bindingResult), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(GlobalExceptionHandler.VALIDATION_CODE);
        assertThat(response.getBody().details()).containsExactly("month: en fazla 12 olmalıdır");
    }

    @Test
    void handleBadRequest_coversTypeMismatchAndIllegalArgument() throws Exception {
        MethodParameter parameter = new MethodParameter(
                DeclarationController.class.getDeclaredMethod("send",
                        DeclarationFilterRequest.class, String.class), 0);
        MethodArgumentTypeMismatchException mismatch = new MethodArgumentTypeMismatchException(
                "ABC", Integer.class, "year", parameter, new NumberFormatException("ABC"));

        ResponseEntity<ErrorResponse> fromMismatch = handler.handleBadRequest(mismatch, request);
        ResponseEntity<ErrorResponse> fromIllegalArgument =
                handler.handleBadRequest(new IllegalArgumentException("geçersiz parametre"), request);

        assertThat(fromMismatch.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(fromIllegalArgument.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(fromIllegalArgument.getBody()).isNotNull();
        assertThat(fromIllegalArgument.getBody().code()).isEqualTo(GlobalExceptionHandler.VALIDATION_CODE);
        assertThat(fromIllegalArgument.getBody().details()).containsExactly("geçersiz parametre");
    }

    @Test
    void handleUnexpected_returns500AndHidesInternals() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("ORA-00942"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(GlobalExceptionHandler.INTERNAL_CODE);
        assertThat(response.getBody().message()).isEqualTo("Beklenmeyen bir hata oluştu.");
    }

    @Test
    void errorResponse_defaultsDetailsToAnEmptyList() {
        ErrorResponse response = ErrorResponse.of("/x", "CODE", "mesaj", null);

        assertThat(response.details()).isEmpty();
    }
}
