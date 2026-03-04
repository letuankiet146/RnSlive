package org.ltk.connector.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class UrlBuilder {
    private static DecimalFormat df;

    public static String joinQueryParameters(String path, Map<String, Object> parameters) {
        StringBuilder urlPath = new StringBuilder(path);
        if (parameters != null && !parameters.isEmpty()) {
//            urlPath.append('?');
            boolean isFirst = true;
            Iterator var3 = parameters.entrySet().iterator();

            while(true) {
                while(var3.hasNext()) {
                    Map.Entry<String, Object> mapElement = (Map.Entry)var3.next();
                    if (mapElement.getValue() instanceof Double) {
                        parameters.replace(mapElement.getKey(), getFormatter().format(mapElement.getValue()));
                    } else if (mapElement.getValue() instanceof ArrayList) {
                        if (!((ArrayList)mapElement.getValue()).isEmpty()) {
                            String key = (String)mapElement.getKey();
                            joinArrayListParameters(key, urlPath, (ArrayList)mapElement.getValue(), isFirst);
                            isFirst = false;
                        }
                        continue;
                    }

                    if (isFirst) {
                        isFirst = false;
                    } else {
                        urlPath.append('&');
                    }

                    urlPath.append((String)mapElement.getKey()).append('=').append(mapElement.getValue().toString());
                }

                return urlPath.toString();
            }
        } else {
            return urlPath.toString();
        }
    }

    private static void joinArrayListParameters(String key, StringBuilder urlPath, ArrayList<?> values, boolean isFirst) {
        Object value;
        for(Iterator var4 = values.iterator(); var4.hasNext(); urlPath.append(key).append('=').append(urlEncode(value.toString()))) {
            value = var4.next();
            if (isFirst) {
                isFirst = false;
            } else {
                urlPath.append('&');
            }
        }

    }

    public static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static DecimalFormat getFormatter() {
        if (null == df) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
            df = new DecimalFormat("#,##0.###", symbols);
            df.setMaximumFractionDigits(30);
            df.setGroupingUsed(false);
        }

        return df;
    }
}
