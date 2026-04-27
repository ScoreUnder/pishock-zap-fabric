package moe.score.pishockzap.backend.model.pishock;

import com.google.gson.annotations.SerializedName;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ApiKey {
    @SerializedName("UserAPIKeyId")
    public int userApiKeyId;
    public Object user;
    @SerializedName("APIKey")
    public String apiKey;
    public String name;
    public String expiry;
    public String generated;
    public Object scopes;
}
