package moe.score.pishockzap.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Gsons {
    public static final Gson gson = new GsonBuilder().create();
    public static final Gson pascalCaseGson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    private Gsons() {
        throw new UnsupportedOperationException();
    }
}
