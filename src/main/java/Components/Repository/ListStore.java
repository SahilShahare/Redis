package Components.Repository;

import Components.Infrastructure.DoubleEndedList;
import Components.Service.RespSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static Constants.CommandConstants.BULK_NULL;

@Slf4j
@Component
public class ListStore {
    private final Map<String, DoubleEndedList<Object>> map;

    @Autowired
    private RespSerializer respSerializer;

    public ListStore() {
        this.map = new ConcurrentHashMap<>();
    }

    public String putRight(String key, List<Object> values) {
        try {
            DoubleEndedList<Object> lst = map.computeIfAbsent(key, k -> new DoubleEndedList<>());
            int n;
            if (values.size() == 1) {
                n = lst.addRight(values.getFirst());
            } else {
                n = lst.addAllRight(values);
            }
            return respSerializer.serializeInteger(n);
        } catch (Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String putLeft(String key, List<Object> values) {
        try {
            DoubleEndedList<Object> lst = map.computeIfAbsent(key, k -> new DoubleEndedList<>());
            int n;
            if (values.size() == 1) {
                n = lst.addLeft(values.getFirst());
            } else {
                n = lst.addAllLeft(values);
            }
            return respSerializer.serializeInteger(n);
        } catch (Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String get(String key, int start, int end) {
        try {
            DoubleEndedList<Object> lst = map.get(key);
            if (lst == null) {
                return respSerializer.serializeList(new ArrayList<>());
            } else {
                List<Object> res = lst.get(start, end);
                return respSerializer.serializeList(res);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String getSize(String key) {
        try {
            DoubleEndedList<Object> lst = map.get(key);
            if (lst == null) {
                return respSerializer.serializeInteger(0);
            } else {
                return respSerializer.serializeInteger(lst.size());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String removeLeft(String key, int count) {
        DoubleEndedList<Object> lst = map.get(key);

        if(lst == null) return BULK_NULL;

        if (count == 1) {
            Object obj = lst.removeLeft();
            if (obj == null) {
                return BULK_NULL;
            } else {
                return respSerializer.serializeObject(obj);
            }
        } else {
            List<Object> objList = lst.removeLeft(count);
            if (objList.isEmpty()) {
                return BULK_NULL;
            } else {
                return respSerializer.serializeList(objList);
            }
        }
    }
    public Object pollLeft(String key) {
        DoubleEndedList<Object> lst = map.get(key);
        if (lst == null) {
            return null;
        }
        return lst.removeLeft();
    }

    public Object getLeft(String key) {
        DoubleEndedList<Object> lst = map.get(key);
        if (lst == null) {
            return null;
        }
        return lst.getLeft();
    }

}
