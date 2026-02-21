package uz.eduplatform.modules.parent.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QRCodeServiceTest {

    private final QRCodeService qrCodeService = new QRCodeService();

    @Test
    void generatePairingQrCode_returnsDataUri() {
        String result = qrCodeService.generatePairingQrCode("ABCD1234");

        assertThat(result).isNotNull();
        assertThat(result).startsWith("data:image/png;base64,");
        assertThat(result.length()).isGreaterThan(100);
    }

    @Test
    void generatePairingQrCode_differentCodes_differentQr() {
        String qr1 = qrCodeService.generatePairingQrCode("ABCD1234");
        String qr2 = qrCodeService.generatePairingQrCode("EFGH5678");

        assertThat(qr1).isNotEqualTo(qr2);
    }
}
