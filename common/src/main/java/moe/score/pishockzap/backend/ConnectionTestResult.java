package moe.score.pishockzap.backend;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public enum ConnectionTestResult {
    UNKNOWN_ERROR,
    SUCCESS,
    NOT_CONFIGURED,
    CONNECTION_FAILED,
    AUTHENTICATION_FAILED,
    PERMISSION_DENIED,
    DEVICE_MISSING,
    TIMED_OUT,
}
