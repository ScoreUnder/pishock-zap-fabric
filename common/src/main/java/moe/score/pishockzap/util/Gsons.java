package moe.score.pishockzap.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;
import moe.score.pishockzap.util.gson.CaseInsensitiveEnumTypeAdapterFactory;
import moe.score.pishockzap.util.gson.Int2ObjectMapFactory;
import moe.score.pishockzap.util.gson.IntListFactory;
import org.jetbrains.annotations.ApiStatus;

@UtilityClass
@ApiStatus.Internal
public class Gsons {
    public static final Gson gson;
    public static final Gson prettyGson;
    public static final Gson pascalCaseGson;

    static {
        var builder = new GsonBuilder()
            .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
            .registerTypeAdapterFactory(new Int2ObjectMapFactory())
            .registerTypeAdapterFactory(new IntListFactory());

        gson = builder.create();
        prettyGson = builder.setPrettyPrinting().create();
        pascalCaseGson = builder.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    }
}
