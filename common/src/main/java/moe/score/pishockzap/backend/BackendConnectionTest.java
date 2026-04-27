package moe.score.pishockzap.backend;

import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.CompletableFuture;

@ApiStatus.Internal
public interface BackendConnectionTest {
    CompletableFuture<ConnectionTestResult> testConnection();

    CompletableFuture<ConnectionTestResult> testVibration();
}
