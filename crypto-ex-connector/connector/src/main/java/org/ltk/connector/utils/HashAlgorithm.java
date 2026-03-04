package org.ltk.connector.utils;

public enum HashAlgorithm {
    HMAC_MD5("HmacMD5")
    ,HMAC_SHA256("HmacSHA256");

    private String algorithm;
    private HashAlgorithm (String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm(){
        return algorithm;
    }
}
