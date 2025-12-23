package moe.arvin.kanonbot.util;

import java.net.URI;
import java.net.URISyntaxException;

public class URLUtil {

    public static boolean isValidURL(String url, String[] allowedSchemes) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            // java.net.URI parses the string and throws URISyntaxException if invalid
            URI uri = new URI(url);
            String scheme = uri.getScheme();

            if (scheme == null) {
                return false;
            }

            for (String allowedScheme : allowedSchemes) {
                if (scheme.equalsIgnoreCase(allowedScheme)) {
                    return true;
                }
            }
        } catch (URISyntaxException e) {
            return false;
        }
        return false;
    }
}
