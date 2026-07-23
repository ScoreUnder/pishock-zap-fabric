package moe.score.pishockzap.util.gson;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;

public final class IntListFactory extends TypeAdapter<IntList> implements TypeAdapterFactory {
    private final TypeAdapter<IntList> nullSafeAdapter = nullSafe();

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!IntList.class.isAssignableFrom(type.getRawType())
            || !type.getRawType().isAssignableFrom(IntArrayList.class)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        var adapter = (TypeAdapter<T>) nullSafeAdapter;
        return adapter;
    }

    @Override
    public void write(JsonWriter out, IntList value) throws IOException {
        out.beginArray();
        // Note: for-each loop would box integers
        var size = value.size();
        for (int i = 0; i < size; i++) {
            out.value(value.getInt(i));
        }
        out.endArray();
    }

    @Override
    public IntList read(JsonReader in) throws IOException {
        var list = new IntArrayList();
        in.beginArray();
        while (in.hasNext()) {
            list.add(readInt(in));
        }
        in.endArray();
        return list;
    }

    private static int readInt(JsonReader in) throws IOException {
        try {
            return in.nextInt();
        } catch (NumberFormatException e) {
            throw new JsonParseException(e);
        }
    }
}
