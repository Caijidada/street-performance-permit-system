package com.pdk.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Component
public class QrCodeUtil {

    public void generate(String content, String filePath) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix matrix = new MultiFormatWriter()
                .encode(content, BarcodeFormat.QR_CODE, 300, 300, hints);

        File file = new File(filePath);
        file.getParentFile().mkdirs();
        Path path = Paths.get(filePath);
        MatrixToImageWriter.writeToPath(matrix, "PNG", path);
    }
}
