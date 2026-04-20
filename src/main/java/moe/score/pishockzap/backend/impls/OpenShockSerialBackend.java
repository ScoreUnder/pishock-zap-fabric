package moe.score.pishockzap.backend.impls;

import com.google.gson.annotations.SerializedName;
import lombok.NonNull;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.SerialBackend;
import moe.score.pishockzap.config.PishockZapConfig;

import java.util.List;
import java.util.concurrent.Executor;

import static moe.score.pishockzap.util.Gsons.gson;

public class OpenShockSerialBackend extends SerialBackend<OpenShockSerialBackend.ShockDevice> {
    public OpenShockSerialBackend(@NonNull PishockZapConfig config, @NonNull Executor executor) {
        super(config, executor);
    }

    @Override
    protected boolean areShockParamsValid(OpType op, int intensity, float duration) {
        return intensity >= 0 && intensity <= 255 && duration > 0 && duration < getMaxDuration();
    }

    @Override
    public float getMaxDuration() {
        return 65.535f;
    }

    @Override
    public boolean isConfigured() {
        // TODO: sharing pishock's config
        if (config.getDeviceIds().isEmpty()) {
            logger.warning("No shocker IDs configured");
            return false;
        }
        if (config.getSerialPort().isBlank()) {
            logger.warning("No serial port configured");
            return false;
        }
        return true;
    }

    @Override
    protected List<ShockDevice> getDevices() {
        return config.getDeviceIds().stream().map(i -> new ShockDevice(ShockCollarModel.CAIXIANLIN, i)).toList();
    }

    @Override
    protected String getOperationData(@NonNull OpType op, int intensity, float duration, ShockDevice device) {
        var payload = new RfTransmitPayload(
            device.model, device.id,
            ShockerCommandType.of(op), intensity, Math.round(duration * 1000));
        // longest valid string without extra spacing:
        // "rftransmit {"model":"petrainer998dr","id":65535,"type":"vibrate","intensity":255,"durationMs":65535}"
        // = 100 chars long
        var sb = new StringBuilder(100);
        sb.append("rftransmit ");
        gson.toJson(payload, sb);
        return sb.toString();
    }

    public record ShockDevice(ShockCollarModel model, int id) {
    }

    public record RfTransmitPayload(
        ShockCollarModel model, int id, ShockerCommandType type, int intensity, int durationMs) {
    }

    public enum ShockCollarModel {
        @SerializedName("caixianlin") CAIXIANLIN,
        @SerializedName("petrainer") PETRAINER,
        @SerializedName("petrainer998dr") PETRAINER_998DR,
        @SerializedName("wellturnt330") WELLTURN_T330,
        @SerializedName("d80") D80,
    }

    public enum ShockerCommandType {
        @SerializedName("shock") SHOCK,
        @SerializedName("vibrate") VIBRATE,
        @SerializedName("sound") SOUND,
        @SerializedName("light") LIGHT,
        @SerializedName("stop") STOP;

        public static ShockerCommandType of(OpType op) {
            return switch (op) {
                case SHOCK -> SHOCK;
                case VIBRATE -> VIBRATE;
                case BEEP -> SOUND;
            };
        }
    }
}
