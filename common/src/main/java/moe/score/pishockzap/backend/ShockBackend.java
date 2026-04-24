package moe.score.pishockzap.backend;

import lombok.NonNull;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.util.TriState;
import org.jetbrains.annotations.ApiStatus;

/**
 * A backend for performing shock operations. This is the raw interface to the shock device, and does not manage timing,
 * queueing, etc.; that is the responsibility of {@link moe.score.pishockzap.frontend.ShockFrontend}.
 * <p>
 * Implementations should be thread-safe; while the frontend uses a dedicated worker thread to invoke
 * {@link #performOp}, the API may be called from other threads by other mods, and calls to anything else (e.g.
 * {@link #close()}) are expected to happen from other threads which may be running at the same time as a
 * {@link #performOp} operation.
 * <p>
 * When implementing: Prefer to subclass {@link SafeShockBackend} unless you have a good reason not to; it provides some
 * basic safety checks including guarantees to the user that they will not be shocked above their limits or when the
 * mod has been disabled.
 */
@ApiStatus.AvailableSince("2.0.0")
@SuppressWarnings("unused")
public interface ShockBackend {
    /**
     * Perform the given operation on the user's shock device(s).
     *
     * @param distribution which devices to activate
     * @param op           which operation to perform
     * @param intensity    the intensity (0-100). note that 99 and 100 are the same on PiShock.
     *                     0 will not operate the device, but (depending on implementation) may stop an ongoing operation.
     * @param duration     the desired length of the operation, in seconds
     * @return {@code false} if the operation was definitely not performed (e.g. bad parameters or kill-switch hotkey)
     */
    boolean performOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration);

    /**
     * The maximum supported duration for this specific backend.
     */
    default float getMaxDuration() {
        return PiShockUtils.PISHOCK_MAX_DURATION;
    }

    /**
     * Whether this backend allows you to replace ongoing operations or not.
     * i.e. if you were to send a shock at 50% for 10 seconds, then before a second passes, send another at 70% for 3
     * seconds, this method would return {@link TriState#TRUE} if that results in the shock being set to 70% at the new
     * duration, {@link TriState#FALSE} if the second shock is ignored, and {@link TriState#UNCERTAIN} if there is no
     * reasonable way to tell.
     */
    @NonNull
    default TriState canReplaceOngoingOperation() {
        return TriState.UNCERTAIN;
    }

    /**
     * Close any resources this backend has open.
     * Not intended for public use; this is for switching out backends via user config.
     */
    @ApiStatus.OverrideOnly
    default void close() {
    }
}
