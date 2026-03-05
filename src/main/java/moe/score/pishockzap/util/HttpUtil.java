package moe.score.pishockzap.util;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    public static CompletableFuture<byte @NonNull []> makeRequestAsync(ThrowingSupplier<URL> urlSupplier, @Nullable ThrowingSupplier<byte @Nullable []> postBodySupplier, ThrowingSupplier<Map<String, String>> headerSupplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var url = urlSupplier.get();
                var postBody = postBodySupplier == null ? null : postBodySupplier.get();
                var headers = headerSupplier.get();
                return makeRequestSync(url, postBody, headers);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static CompletableFuture<String> makeRequestAsyncUtf8(ThrowingSupplier<URL> urlSupplier, @Nullable ThrowingSupplier<String> postBodySupplier, ThrowingSupplier<Map<String, String>> headerSupplier) {
        return makeRequestAsync(urlSupplier, () -> {
            if (postBodySupplier == null)return null;
            var postBody = postBodySupplier.get();
            if (postBody == null) return null;
            return postBody.getBytes(StandardCharsets.UTF_8);
        }, headerSupplier).thenApply(b -> new String(b, StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    public static interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
