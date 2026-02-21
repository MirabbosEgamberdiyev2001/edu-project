package uz.eduplatform.modules.parent.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePairingCodeResponse {

    private String pairingCode;
    private LocalDateTime expiresAt;
    private String qrCodeDataUri;
}
