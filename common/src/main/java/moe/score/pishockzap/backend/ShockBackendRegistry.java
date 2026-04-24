package moe.score.pishockzap.backend;

import lombok.experimental.UtilityClass;
import moe.score.pishockzap.config.PishockZapConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

@UtilityClass
public class ShockBackendRegistry {
    private static final Map<String, BiFunction<? super PishockZapConfig, ? super Executor, ? extends ShockBackend>> backendFactories = new HashMap<>();
    private static final Map<String, String> translationKeys = new HashMap<>();

    public static void register(String id, String translationKey, BiFunction<? super PishockZapConfig, ? super Executor, ? extends ShockBackend> factory) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(translationKey);
        Objects.requireNonNull(factory);
        synchronized (backendFactories) {
            backendFactories.put(id, factory);
            translationKeys.put(id, translationKey);
        }
    }

    public static BiFunction<? super PishockZapConfig, ? super Executor, ? extends ShockBackend> getCreateFunc(String id) {
        synchronized (backendFactories) {
            return backendFactories.get(id);
        }
    }

    public static String getTranslationKey(String id) {
        synchronized (backendFactories) {
            return translationKeys.get(id);
        }
    }

    public static String[] getAllBackendIds() {
        synchronized (backendFactories) {
            return backendFactories.keySet().toArray(String[]::new);
        }
    }
}
