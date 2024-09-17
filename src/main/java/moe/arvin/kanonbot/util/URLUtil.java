package moe.arvin.kanonbot.util;

import org.springframework.web.util.UriComponentsBuilder;

public class URLUtil {

    public static boolean isValidURL(String url, String[] allowedSchemes) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            String scheme = builder.build().getScheme();

            if (scheme == null) {
                return false;
            }

            for (String allowedScheme : allowedSchemes) {
                if (scheme.equalsIgnoreCase(allowedScheme)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

}
