package org.ltk.connector.utils;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HashAlgorithmTest {
    @Test
    public void testHMAC() {
        String expectedValue = "82cdc26fc9d4154946f2ce7f228813143cb07d57a0b8e526660136d2c644421b";
        String payload = "abc";
        String key = "private-key";
        String hmacVal = SecurityHelper.hmac(HashAlgorithm.HMAC_SHA256, payload, key);
        System.out.println(hmacVal);
        Assertions.assertNotNull(hmacVal);
        Assertions.assertEquals(expectedValue, hmacVal);
    }

    @Test
    public void testGenSignature_BingX() {
        String expectedValue = "f8d883609dfd31c824feb4de865b071008dedb1d461451fa70847875c8e7a7a2";
        String payload = "recvWindow=0&symbol=BTC-USDT&timestamp=1696751141337";
        String exampleSecretKey = "mheO6dR8ovSsxZQCOYEFCtelpuxcWGTfHw7te326y6jOwq5WpvFQ9JNljoTwBXZGv5It07m9RXSPpDQEK2w";
        String hmacVal = SecurityHelper.hmac(HashAlgorithm.HMAC_SHA256, payload, exampleSecretKey);
        System.out.println(hmacVal);
        Assertions.assertNotNull(hmacVal);
        Assertions.assertEquals(expectedValue, hmacVal);
    }

    @Test
    public void testCalcOkxChecksum() {
        String payload = "70705:739.98:70705.1:119.54:70704.9:0.06:70705.2:11.26:70704.8:0.05:70705.3:0.05:70704.6:0.06:70705.5:0.05:70704.5:19.33:70705.6:0.05:70704.3:0.06:70705.7:0.01:70704.2:22.19:70705.8:0.07:70704.1:10.97:70705.9:0.05:70704:5.28:70706:0.08:70703.9:0.05:70706.2:0.05:70703.8:0.05:70706.3:0.05:70703.6:0.06:70706.5:0.05:70703.5:0.06:70706.6:0.08:70703.4:0.05:70706.7:0.06:70703.3:0.03:70706.8:0.02:70703.2:0.05:70706.9:1.61:70703.1:0.2:70707:5.19:70703:5.13:70707.2:0.05:70702.9:0.05:70707.3:0.08:70702.8:0.05:70707.5:0.75:70702.6:0.08:70707.6:0.07:70702.5:0.05:70707.7:0.06:70702.4:0.06:70707.8:0.1:70702.2:0.05:70707.9:0.06:70702.1:0.75:70708:5.18";
        int expectValue = -1657813722;
        int actualValue = SecurityHelper.calcOkxChecksum(payload);
        Assertions.assertEquals(expectValue, actualValue);
    }
}
