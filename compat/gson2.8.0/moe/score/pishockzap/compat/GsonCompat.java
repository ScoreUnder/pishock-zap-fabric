package moe.score.pishockzap.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;

import java.io.Reader;

@UtilityClass
public class GsonCompat {
    public static JsonElement parse(Reader reader) {
        return new JsonParser().parse(reader);
    }
}
