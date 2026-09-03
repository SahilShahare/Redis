package Components.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RespSerializer {

    public String serializeBulkString(String s) {
        int length = s.length();
        String respHeader = "$" + length;
        return respHeader + "\r\n" + s + "\r\n";
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
}
