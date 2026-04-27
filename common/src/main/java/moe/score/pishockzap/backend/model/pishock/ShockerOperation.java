package moe.score.pishockzap.backend.model.pishock;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class ShockerOperation {
    public String username;
    public String code;
    public String name;
    @SerializedName("Apikey")
    public String apiKey;
    public int op;
    public int intensity;
    public int duration;
}
