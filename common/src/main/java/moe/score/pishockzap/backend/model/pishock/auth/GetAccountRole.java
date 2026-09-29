package moe.score.pishockzap.backend.model.pishock.auth;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/// From [PiShock Auth API](https://auth.pishock.com/swagger/v0/swagger.json)
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC)
public class GetAccountRole {
    @SerializedName("UserAccessPermissionId")
    int userAccessPermissionId;
    @SerializedName("Permission")
    AccessPermission permission;
}
