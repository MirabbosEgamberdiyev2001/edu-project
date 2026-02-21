package uz.eduplatform.modules.subscription.dto.payme;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymeError {

    public static final int INVALID_AMOUNT = -31001;
    public static final int ORDER_NOT_FOUND = -31050;
    public static final int CANT_PERFORM = -31008;
    public static final int TRANSACTION_NOT_FOUND = -31003;
    public static final int INVALID_AUTH = -32504;
    public static final int METHOD_NOT_FOUND = -32601;

    private int code;
    private Map<String, String> message;
    private String data;

    public static PaymeError of(int code, String messageRu, String messageEn) {
        return PaymeError.builder()
                .code(code)
                .message(Map.of("ru", messageRu, "en", messageEn))
                .build();
    }

    public static PaymeError invalidAuth() {
        return of(INVALID_AUTH, "Ошибка авторизации", "Authorization error");
    }

    public static PaymeError orderNotFound() {
        return of(ORDER_NOT_FOUND, "Заказ не найден", "Order not found");
    }

    public static PaymeError invalidAmount() {
        return of(INVALID_AMOUNT, "Неверная сумма", "Invalid amount");
    }

    public static PaymeError cantPerform() {
        return of(CANT_PERFORM, "Невозможно выполнить операцию", "Cannot perform operation");
    }

    public static PaymeError transactionNotFound() {
        return of(TRANSACTION_NOT_FOUND, "Транзакция не найдена", "Transaction not found");
    }

    public static PaymeError methodNotFound() {
        return of(METHOD_NOT_FOUND, "Метод не найден", "Method not found");
    }
}
