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
}
