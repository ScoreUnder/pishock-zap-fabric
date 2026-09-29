package moe.score.pishockzap.backend.model.pishock.auth;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/// From [PiShock Auth API](https://auth.pishock.com/swagger/v0/swagger.json)
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC)
public class GetAccountImage {
    @SerializedName("ImageId")
    int imageId;
    @SerializedName("ImageBase64")
    String imageBase64;
    @SerializedName("ImageType")
    AccountImageType imageType;
}
