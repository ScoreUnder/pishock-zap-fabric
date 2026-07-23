package moe.score.pishockzap.util.gson;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public final class Int2ObjectMapAdapter<T> extends TypeAdapter<Int2ObjectMap<T>> {
    private final TypeAdapter<T> valueAdapter;

    @Override
    public void write(JsonWriter out, Int2ObjectMap<T> map) throws IOException {
        out.beginObject();
        for (var entry : map.int2ObjectEntrySet()) {
            out.name(Integer.toString(entry.getIntKey()));
            valueAdapter.write(out, entry.getValue());
        }
        out.endObject();
    }

    @Override
    public Int2ObjectMap<T> read(JsonReader in) throws IOException {
        var map = new Int2ObjectArrayMap<T>();
        in.beginObject();
        while (in.hasNext()) {
            int key = readIntName(in);
            T value = valueAdapter.read(in);
            map.put(key, value);
        }
        in.endObject();
        return map;
    }

    private static int readIntName(JsonReader in) throws IOException {
        String name = in.nextName();
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            throw new JsonParseException("Expected an integer key but was \"" + name + "\". " + in, e);
        }
    }
}
