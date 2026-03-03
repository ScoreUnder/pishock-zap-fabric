package moe.score.pishockzap.backend;

import lombok.NonNull;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.util.TriState;

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
    default void close() {
    }
}
