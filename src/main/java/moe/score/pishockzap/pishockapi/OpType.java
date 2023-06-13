package moe.score.pishockzap.pishockapi;

public enum OpType {
    SHOCK(0), VIBRATE(1), BEEP(2) /*, LED(???)*/;

    public final int code;

    OpType(int code) {
        this.code = code;
    }
}
