package Components.Service;

import Components.Infrastructure.StreamEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RespSerializer {

    public String serializeBulkString(String s) {
        int length = s.length();
        String respHeader = "$" + length;
        return respHeader + "\r\n" + s + "\r\n";
    }

    public String serializeSimpleString(String s) {
        return "+" + s + "\r\n";
    }

    public String serializeInteger(int n) {
        return ":" + n + "\r\n";
    }

    public String serializeObject(Object o) {
        return switch (o) {
            case Integer i -> serializeInteger(i);
            case String s -> serializeBulkString(s);
            default -> "";
        };
    }

    public String serializeBlpopReply(String key, Object value) {
        return "*2\r\n" + serializeBulkString(key) + serializeObject(value);
    }

    public String serializeList(List<Object> lst) {
        StringBuilder res = new StringBuilder("*");
        res.append(lst.size()).append("\r\n");
        for (Object o : lst) {
            res.append(serializeObject(o));
        }
        return res.toString();
    }

    public String serializeError(String message) {
        return "-" + message + "\r\n";
    }

    public String serializeStreamEntry(StreamEntry entry) {
        StringBuilder res = new StringBuilder("*2\r\n");
        res.append(serializeBulkString(entry.id.toString()));
        res.append("*").append(entry.fields.size() * 2).append("\r\n");
        for (Map.Entry<String, String> field : entry.fields.entrySet()) {
            res.append(serializeBulkString(field.getKey()));
            res.append(serializeBulkString(field.getValue()));
        }
        return res.toString();
    }

    public String serializeStreamEntries(List<StreamEntry> entries) {
        StringBuilder res = new StringBuilder("*").append(entries.size()).append("\r\n");
        for (StreamEntry entry : entries) {
            res.append(serializeStreamEntry(entry));
        }
        return res.toString();
    }

    public String serializeStreamReadEntry(String key, List<StreamEntry> entries) {
        return "*2\r\n" + serializeBulkString(key) + serializeStreamEntries(entries);
    }
}
