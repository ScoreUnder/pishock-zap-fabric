package moe.score.pishockzap.backend;

import lombok.NonNull;
import moe.score.pishockzap.config.ShockDistribution;

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

    default float getMaxDuration() {
        return PiShockUtils.PISHOCK_MAX_DURATION;
    }

    /**
     * Close any resources this backend has open.
     * Not intended for public use; this is for switching out backends via user config.
     */
    default void close() {
    }
}
