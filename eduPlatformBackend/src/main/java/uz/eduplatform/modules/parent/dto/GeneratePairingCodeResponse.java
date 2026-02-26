package uz.eduplatform.modules.parent.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePairingCodeResponse {

    /** Pairing code (8 characters, e.g. "AB3X7K9M") */
    private String code;

    /** Seconds until the code expires (for countdown timer) */
    private long expiresIn;

    /** Base64 PNG data URI of the QR code */
    private String qrCodeDataUri;

    /** Convenience factory that computes expiresIn from an absolute expiry timestamp */
    public static GeneratePairingCodeResponse of(String code, LocalDateTime expiresAt, String qrCodeDataUri) {
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), expiresAt);
        return GeneratePairingCodeResponse.builder()
                .code(code)
                .expiresIn(Math.max(0, seconds))
                .qrCodeDataUri(qrCodeDataUri)
                .build();
    }
}
