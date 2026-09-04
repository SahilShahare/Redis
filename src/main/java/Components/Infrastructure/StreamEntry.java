package Components.Infrastructure;

import java.util.LinkedHashMap;

public class StreamEntry {
    public final StreamId id;
    public final LinkedHashMap<String, String> fields;

    public StreamEntry(StreamId id, LinkedHashMap<String, String> fields) {
        this.id = id;
        this.fields = fields;
    }
}