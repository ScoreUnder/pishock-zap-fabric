package moe.score.pishockzap.util.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.apache.commons.lang3.reflect.TypeUtils;

public final class Int2ObjectMapFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (!Int2ObjectMap.class.isAssignableFrom(rawType)
            || !rawType.isAssignableFrom(Int2ObjectArrayMap.class)) {
            return null;
        }

        var typeArgs = TypeUtils.getTypeArguments(type.getType(), Int2ObjectMap.class);
        if (typeArgs == null) return null;
        var valueType = typeArgs.get(Int2ObjectMap.class.getTypeParameters()[0]);
        if (valueType == null) return null;
        var valueAdapter = gson.getAdapter(TypeToken.get(valueType));
        @SuppressWarnings("unchecked")
        var mapAdapter = (TypeAdapter<T>) new Int2ObjectMapAdapter<>(valueAdapter);
        return mapAdapter.nullSafe();
    }
}
