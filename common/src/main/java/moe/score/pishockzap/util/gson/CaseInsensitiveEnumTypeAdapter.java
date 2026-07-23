package moe.score.pishockzap.util.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CaseInsensitiveEnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
    private final Map<String, T> nameToConstant = new HashMap<>();
    private final Map<T, String> constantToName = new HashMap<>();

    public CaseInsensitiveEnumTypeAdapter(Class<T> classOfT) {
        try {
            for (final Field field : classOfT.getDeclaredFields()) {
                if (!field.isEnumConstant()) continue;
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
        return nameToConstant.get(toLower(in.nextString()));
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        out.value(constantToName.get(value));
    }

    private String toLower(String s) {
        return s.toLowerCase(Locale.US);
    }
}
