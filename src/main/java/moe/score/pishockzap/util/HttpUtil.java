package moe.score.pishockzap.util;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

public class HttpUtil {
    public static byte @NonNull [] makeRequestSync(@NonNull URL url, byte @Nullable [] postBody, @NonNull Map<String, String> headers) throws IOException {
        URLConnection connection = url.openConnection();
        if (postBody != null) {
            connection.setDoOutput(true);
        }

        for (var header : headers.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }

        if (postBody != null) {
            try (var outputStream = connection.getOutputStream()) {
                outputStream.write(postBody);
            }
        }

        byte[] response;
        try (var inputStream = connection.getInputStream()) {
            response = inputStream.readAllBytes();
        }
        return response;
    }
}
