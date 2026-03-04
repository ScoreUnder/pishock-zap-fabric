package moe.score.pishockzap.backend;

import lombok.NonNull;

public enum OpType {
    SHOCK(0, "shock"),
    VIBRATE(1, "vibrate"),
    BEEP(2, "beep");

    public final int code;
    @NonNull
    public final String firmwareCode;

    OpType(int code, @NonNull String firmwareCode) {
        this.code = code;
        this.firmwareCode = firmwareCode;
    }
}
