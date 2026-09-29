package moe.score.pishockzap.backend.model.pishock.auth;

import com.google.gson.annotations.SerializedName;

/// From [PiShock Auth API](https://auth.pishock.com/swagger/v0/swagger.json)
public enum AccountType {
    @SerializedName("0") CLASSIC,
    @SerializedName("1") DISCORD,
    @SerializedName("2") GMAIL,
    @SerializedName("3") TELEGRAM,
    @SerializedName("4") TWITCH,
    @SerializedName("5") APPLE
}
