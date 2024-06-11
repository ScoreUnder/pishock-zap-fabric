package moe.score.pishockzap.pishockapi;

public enum OpType {
    SHOCK(0, "shock"),
    VIBRATE(1, "vibrate"),
    BEEP(2, "beep");

    public final int code;
    public final String firmwareCode;

    OpType(int code, String firmwareCode) {
        this.code = code;
        this.firmwareCode = firmwareCode;
    }
}
