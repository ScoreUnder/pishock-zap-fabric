package moe.score.pishockzap.backend.model.pishock;

import com.google.gson.annotations.SerializedName;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
public class UserProfile {
    public int userId;
    public String username;
    public String lastLogin;
    public String password;
    @SerializedName("IPAddress")
    public String ipAddress;
    public Object sessions;
    public Object emails;
    @SerializedName("APIKeys")
    public List<ApiKey> apiKeys;
    public Object oAuthLinks;
    public Object images;
    public Object accessPermissions;
}
