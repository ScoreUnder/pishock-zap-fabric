package moe.score.pishockzap.backend.model.pishock.api;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/// Undocumented :(
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC)
public class ShockerInfo {
    @SerializedName("HubId")
    int hubId;
    @SerializedName("ShockerId")
    int shockerId;
    @SerializedName("Name")
    String name;
    @SerializedName("IsV3")
    boolean isV3;
}
