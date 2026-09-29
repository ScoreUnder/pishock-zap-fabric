package moe.score.pishockzap.backend.model.pishock.auth;

import com.google.gson.annotations.SerializedName;

/// From [PiShock Auth API](https://auth.pishock.com/swagger/v0/swagger.json)
public enum AccountImageType {
    @SerializedName("0") PROFILE,
    @SerializedName("1") BANNER
}
