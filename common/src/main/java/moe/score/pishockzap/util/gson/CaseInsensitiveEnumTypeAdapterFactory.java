package moe.score.pishockzap.util.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class CaseInsensitiveEnumTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        var rawType = type.getRawType();
        if (!rawType.isEnum()) return null;

        @SuppressWarnings({"rawtypes", "unchecked"})
        var adapter = (TypeAdapter<T>) new CaseInsensitiveEnumTypeAdapter(rawType);
        return adapter.nullSafe();
    }
}
