package moe.score.pishockzap.backend.model.pishock;

import com.google.gson.annotations.SerializedName;
import moe.score.pishockzap.backend.OpType;

public enum V2OperationType {
    @SerializedName("v") VIBRATE,
    @SerializedName("s") SHOCK,
    @SerializedName("b") BEEP,
    @SerializedName("e") STOP;

    public static V2OperationType of(OpType type) {
        return switch (type) {
            case SHOCK -> SHOCK;
            case VIBRATE -> VIBRATE;
            case BEEP -> BEEP;
        };
    }
}
