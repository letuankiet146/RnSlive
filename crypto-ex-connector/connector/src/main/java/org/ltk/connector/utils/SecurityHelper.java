package org.ltk.connector.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class SecurityHelper {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String hmac(HashAlgorithm algorithm, String data, String key) {
        if (data == null || key == null) {
            throw new IllegalArgumentException("Data or key is null");
        }
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), algorithm.getAlgorithm());
            Mac mac = Mac.getInstance(algorithm.getAlgorithm());
            mac.init(secretKeySpec);
            return bytesToHex(mac.doFinal(data.getBytes())).toLowerCase();
        } catch (Exception e) {
            throw new IllegalArgumentException("HMAC failed");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static int calcOkxChecksum(String checkStr) {
        CRC32 crc32 = new CRC32();
        crc32.update(checkStr.getBytes(StandardCharsets.US_ASCII));
        return (int) crc32.getValue();
    }
}
