package moe.score.pishockzap.backend.model.pishock.auth;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/// From [PiShock Auth API](https://auth.pishock.com/swagger/v0/swagger.json)
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC)
public class GetAccountEmail {
    @SerializedName("EmailId")
    int emailId;
    @SerializedName("Email")
    String email;
    @SerializedName("Confirmed")
    boolean confirmed;
    @SerializedName("Primary")
    boolean primary;
}
