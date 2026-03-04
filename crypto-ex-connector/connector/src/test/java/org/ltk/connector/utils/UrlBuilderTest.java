package org.ltk.connector.utils;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

public class UrlBuilderTest {

    @Test
    public void testJoinQueryParameters() {
        String expectedValue = "q1=v1&q2=v2&q3=v3";
        LinkedHashMap<String, Object> queryParam = new LinkedHashMap<>();
        queryParam.put("q1", "v1");
        queryParam.put("q2", "v2");
        queryParam.put("q3", "v3");
        String out = UrlBuilder.joinQueryParameters("", queryParam);
        Assertions.assertEquals(expectedValue, out);
    }
}
