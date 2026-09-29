package moe.score.pishockzap.backend.model.pishock.auth;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Nullable;

/// From [PiShock Auth API](https://auth.pishock.com/swagger/v0/swagger.json)
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC)
public class GetAccountApiKey {
    @SerializedName("ApiKeyId")
    int apiKeyId;
    @SerializedName("Name")
    String name;
    @SerializedName("Expiry")
    @Nullable
    String expiry; // date-time
    @SerializedName("Generated")
    String generated; // date-time
}
