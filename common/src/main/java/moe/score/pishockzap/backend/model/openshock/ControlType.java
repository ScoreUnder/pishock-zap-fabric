package moe.score.pishockzap.backend.model.openshock;

import com.google.gson.annotations.SerializedName;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.impls.OpenShockWebApiBackend;

public enum ControlType {
    @SerializedName("Stop") STOP,
    @SerializedName("Shock") SHOCK,
    @SerializedName("Vibrate") VIBRATE,
    @SerializedName("Sound") SOUND;

    public static ControlType of(OpType op) {
        return switch (op) {
            case SHOCK -> SHOCK;
            case BEEP -> SOUND;
            case VIBRATE -> VIBRATE;
        };
    }
}
