package uz.eduplatform.modules.notification.client;

public record SendResult(boolean success, String providerResponse, String errorMessage) {

    public static SendResult ok(String providerResponse) {
        return new SendResult(true, providerResponse, null);
    }

    public static SendResult fail(String errorMessage) {
        return new SendResult(false, null, errorMessage);
    }
}
