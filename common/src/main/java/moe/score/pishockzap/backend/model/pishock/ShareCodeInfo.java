package moe.score.pishockzap.backend.model.pishock;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ShareCodeInfo {
    public int shareId;
    public int clientId;
    public int shockerId;
    public String shockerName;
    public boolean isPaused;
    public int maxIntensity;
    public boolean canContinuous;
    public boolean canShock;
    public boolean canVibrate;
    public boolean canBeep;
    public boolean canLog;
    public String shareCode;
}
