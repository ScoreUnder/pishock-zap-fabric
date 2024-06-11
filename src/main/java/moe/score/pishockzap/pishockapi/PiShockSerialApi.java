package moe.score.pishockzap.pishockapi;

import moe.score.pishockzap.config.ShockDistribution;

public class PiShockSerialApi implements PiShockApi {
    private final String portName;

    public PiShockSerialApi(String portName) {
        this.portName = portName;
    }

    @Override
    public void performOp(ShockDistribution distribution, OpType op, int intensity, float duration) {

    }
}
