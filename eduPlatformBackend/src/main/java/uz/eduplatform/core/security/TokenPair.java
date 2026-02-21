package uz.eduplatform.core.security;

public record TokenPair(
        String accessToken,
        String refreshToken,
        String accessJti,
        String refreshJti,
        long accessExpiresIn
) {
}
