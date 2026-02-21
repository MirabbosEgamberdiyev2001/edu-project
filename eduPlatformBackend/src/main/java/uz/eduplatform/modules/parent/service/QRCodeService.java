package uz.eduplatform.modules.parent.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class QRCodeService {

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private static final int QR_WIDTH = 300;
    private static final int QR_HEIGHT = 300;

    /**
     * Generate a QR code image as a base64-encoded data URI.
     * The QR code contains a deep link URL that the parent app can scan.
     */
    public String generatePairingQrCode(String pairingCode) {
        String content = frontendBaseUrl + "/parent/pair?code=" + pairingCode;
        return generateQrCodeDataUri(content);
    }

    private String generateQrCodeDataUri(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, 1
            );

            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code for content: {}", content, e);
            return null;
        }
    }
}
