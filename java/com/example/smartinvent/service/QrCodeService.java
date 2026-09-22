package com.example.smartinvent.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrCodeService {
    public static final String UPI_ID = "7868022337@ybl";
    private static final String MERCHANT_NAME = "Fruit & Jar Works";

    public Map<String, String> generate(double amount, String invoiceNumber) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new RuntimeException("A valid payment amount is required to generate the QR code");
        }
        BigDecimal roundedAmount = BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP);
        String payload = "upi://pay?pa=" + encode(UPI_ID)
                + "&pn=" + encode(MERCHANT_NAME)
                + "&am=" + encode(roundedAmount.toPlainString())
                + "&cu=INR"
                + "&tn=" + encode("Invoice " + (invoiceNumber == null ? "POS" : invoiceNumber));
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 320, 320, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            Map<String, String> result = new HashMap<>();
            result.put("upiId", UPI_ID);
            result.put("payload", payload);
            result.put("amount", roundedAmount.toPlainString());
            result.put("qrCodeDataUrl",
                    "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray()));
            return result;
        } catch (Exception exception) {
            throw new RuntimeException("Unable to generate the payment QR code", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
