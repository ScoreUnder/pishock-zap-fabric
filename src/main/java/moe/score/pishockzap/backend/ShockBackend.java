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
     */
    void performOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration);

    /**
     * Check if this backend is sufficiently configured to be used.
     * May log warnings.
     */
    boolean isConfigured();

    /**
     * Close any resources this backend has open.
     * Not intended for public use; this is for switching out backends via user config.
     */
    default void close() {
    }
}
