package moe.score.pishockzap.util;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@UtilityClass
@ApiStatus.Internal
public class Gsons {
    public static final Gson gson = new GsonBuilder().registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory()).create();
    public static final Gson pascalCaseGson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    private static class CaseInsensitiveEnumTypeAdapterFactory implements TypeAdapterFactory {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<T> rawType = (Class<T>) type.getRawType();
            if (!rawType.isEnum()) {
                return null;
            }

            return new CaseInsensitiveEnumTypeAdapter(rawType);
        }
    }

    private static final class CaseInsensitiveEnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
        private final Map<String, T> nameToConstant = new HashMap<String, T>();
        private final Map<T, String> constantToName = new HashMap<T, String>();

        public CaseInsensitiveEnumTypeAdapter(Class<T> classOfT) {
            try {
                for (final Field field : classOfT.getDeclaredFields()) {
                    if (!field.isEnumConstant()) {
                        continue;
                    }
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    T constant = (T) (field.get(null));
                    String name = constant.name();
                    SerializedName annotation = field.getAnnotation(SerializedName.class);
                    if (annotation != null) {
                        name = annotation.value();
                        for (String alternate : annotation.alternate()) {
                            nameToConstant.put(toLower(alternate), constant);
                        }
                    }
                    nameToConstant.put(toLower(name), constant);
                    constantToName.put(constant, name);
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return nameToConstant.get(toLower(in.nextString()));
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            out.value(value == null ? null : constantToName.get(value));
        }

        private String toLower(String s) {
            return s.toLowerCase(Locale.US);
        }
    }
}
