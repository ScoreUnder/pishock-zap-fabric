package moe.score.pishockzap.backend.model.pishock.auth;

import com.google.gson.annotations.SerializedName;

/// From [PiShock Auth API](https://auth.pishock.com/swagger/v0/swagger.json)
public enum AccessPermission {
    @SerializedName("0") ADMIN,
    @SerializedName("1") QA,
    @SerializedName("2") API_BAN
}
