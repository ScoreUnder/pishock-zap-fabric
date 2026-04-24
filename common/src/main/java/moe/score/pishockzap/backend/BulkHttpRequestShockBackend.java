package moe.score.pishockzap.backend;

import lombok.NonNull;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.concurrent.Executor;

@ApiStatus.Internal
public abstract class BulkHttpRequestShockBackend<U> extends SimpleHttpRequestShockBackend<ShockDistribution, U> {
    public BulkHttpRequestShockBackend(PishockZapConfig config, Executor executor) {
        super(config, executor);
    }

    @Override
    protected @NonNull List<ShockDistribution> getDevices() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void safePerformOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        doApiCallOnThread(generateDataForOperation(distribution, op, intensity, duration));
    }
}
