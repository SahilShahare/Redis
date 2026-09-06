package redis.store;

import redis.infrastructure.DoubleEndedList;
import redis.infrastructure.InvalidTypeException;
import redis.infrastructure.Pair;
import redis.infrastructure.Stream;
import redis.infrastructure.StreamEntry;
import redis.infrastructure.StreamId;
import redis.infrastructure.Type;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@Component
public class Store {
    private final HashMap<String, Value> map = new HashMap<>();

    private final HashMap<String, Long> keyVersions = new HashMap<>();

    public long getVersion(String key) {
        return keyVersions.getOrDefault(key, 0L);
    }

    private void touch(String key) {
        keyVersions.merge(key, 1L, Long::sum);
    }

    public String type(String key) {
        Value value = map.get(key);
        if (value == null) {
            return "none";
        } else {
            return value.getType().getObjectType();
        }
    }

    public void set(String key, String val) {
        StringValue stringValue = new StringValue(val, LocalDateTime.now(), LocalDateTime.MAX);
        map.put(key, stringValue);
        touch(key);
    }

    public void set(String key, String val, int expiryMilliSeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime exp = now.plus(expiryMilliSeconds, ChronoUnit.MILLIS);
        StringValue stringValue = new StringValue(val, now, exp);
        map.put(key, stringValue);
        touch(key);
    }

    public Optional<String> get(String key) throws InvalidTypeException {
        LocalDateTime now = LocalDateTime.now();
        Value value = map.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value.getType() == Type.STRING) {
            StringValue stringValue = (StringValue) value;
            if (stringValue.expiry.isBefore(now)) {
                map.remove(key);
                return Optional.empty();
            } else {
                return Optional.of(stringValue.val);
            }
        } else {
            throw wrongTypeError();
        }
    }

    public int incr(String key) throws NumberFormatException, InvalidTypeException {
        Value value = map.get(key);
        if (value == null) {
            LocalDateTime now = LocalDateTime.now();
            StringValue stringValue = new StringValue("1", now, LocalDateTime.MAX);
            map.put(key, stringValue);
            touch(key);
            return 1;
        }

        if (value.getType() == Type.STRING) {
            StringValue stringValue = (StringValue) value;
            try {
                int valInt = Integer.parseInt(stringValue.val);
                stringValue.val = String.valueOf(valInt + 1);
                touch(key);
                return valInt + 1;
            } catch (NumberFormatException e) {
                throw new NumberFormatException("ERR value is not an integer or out of range");
            }
        } else {
            throw wrongTypeError();
        }
    }

    public int rpush(String key, List<Object> values) throws InvalidTypeException {
        Value value = map.computeIfAbsent(key, k -> new ListValue());
        if (value.getType() == Type.LIST) {
            DoubleEndedList<Object> lst = ((ListValue) value).list;
            int n;
            if (values.size() == 1) {
                n = lst.addRight(values.getFirst());
            } else {
                n = lst.addAllRight(values);
            }
            touch(key);
            return n;
        } else {
            throw wrongTypeError();
        }
    }

    public int lpush(String key, List<Object> values) throws InvalidTypeException {
        Value value = map.computeIfAbsent(key, k -> new ListValue());
        if (value.getType() == Type.LIST) {
            DoubleEndedList<Object> lst = ((ListValue) value).list;
            int n;
            if (values.size() == 1) {
                n = lst.addLeft(values.getFirst());
            } else {
                n = lst.addAllLeft(values);
            }
            touch(key);
            return n;
        } else {
            throw wrongTypeError();
        }
    }

    public List<Object> lrange(String key, int start, int end) throws InvalidTypeException {
        Value value = map.get(key);
        if (value == null) {
            return new ArrayList<>();
        }

        if (value.getType() == Type.LIST) {
            DoubleEndedList<Object> lst = ((ListValue) value).list;
            if (lst == null) {
                return new ArrayList<>();
            } else {
                return lst.get(start, end);
            }
        } else {
            throw wrongTypeError();
        }
    }

    public int llen(String key) throws InvalidTypeException {
        Value value = map.get(key);
        if (value == null) {
            return 0;
        }
        if (value.getType() == Type.LIST) {
            DoubleEndedList<Object> lst = ((ListValue) value).list;
            if (lst == null) {
                return 0;
            } else {
                return lst.size();
            }
        } else {
            throw wrongTypeError();
        }
    }

    public Optional<List<Object>> lpop(String key, int count) throws InvalidTypeException {
        Value value = map.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value.getType() == Type.LIST) {
            DoubleEndedList<Object> lst = ((ListValue) value).list;
            if (lst == null) return Optional.of(new ArrayList<>());
            List<Object> objList = lst.removeLeft(count);
            if (!objList.isEmpty()) {
                touch(key);
            }
            if (lst.isEmpty()) {
                map.remove(key);
            }
            return Optional.of(objList);
        } else {
            throw wrongTypeError();
        }
    }

    public Optional<Object> lpop(String key) throws InvalidTypeException {
        Value value = map.get(key);
        if (value == null) {
            return Optional.empty();
        }

        if (value.getType() == Type.LIST) {
            DoubleEndedList<Object> lst = ((ListValue) value).list;

            if (lst == null) return Optional.empty();

            Object obj = lst.removeLeft();
            if (obj != null) {
                touch(key);
            }
            if (lst.isEmpty()) {
                map.remove(key);
            }
            return (obj == null) ? Optional.empty() : Optional.of(obj);
        } else {
            throw wrongTypeError();
        }
    }

    public String xadd(String key, String idSpec, LinkedHashMap<String, String> fields) throws IllegalArgumentException,
            InvalidTypeException {
        Value value = map.computeIfAbsent(key, k -> new StreamValue());

        if (value.getType() == Type.STREAM) {
            Stream stream = ((StreamValue) value).stream;
            String id = stream.xadd(idSpec, fields);
            touch(key);
            return id;
        } else {
            throw wrongTypeError();
        }
    }

    public List<StreamEntry> xrange(String key, String startSpec, String endSpec) throws InvalidTypeException {
        Value value = map.get(key);
        if (value == null) {
            return new ArrayList<>();
        }
        if (value.getType() == Type.STREAM) {
            Stream stream = ((StreamValue) value).stream;
            return stream.xrange(startSpec, endSpec);
        } else {
            throw wrongTypeError();
        }
    }

    public List<Pair<String, List<StreamEntry>>> xread(List<String> keys, List<String> ids)
            throws InvalidTypeException {
        List<Pair<String, List<StreamEntry>>> parts = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);

            Value value = map.get(key);
            if (value == null) {
                continue;
            }

            if (value.getType() == Type.STREAM) {
                Stream stream = ((StreamValue) value).stream;
                List<StreamEntry> entries = stream.xread(ids.get(i));
                if (!entries.isEmpty()) {
                    parts.add(new Pair<>(key, entries));
                }
            } else {
                throw wrongTypeError();
            }
        }

        return parts;
    }

    //Non-command methods

    public Object pollListLeft(String key) {
        Value value = map.get(key);
        if (value == null) {
            return null;
        }

        if (value.getType() == Type.LIST) {
            DoubleEndedList<Object> lst = ((ListValue) value).list;
            if (lst == null) {
                return null;
            }
            Object obj = lst.removeLeft();
            if (obj != null) {
                touch(key);
            }
            if (lst.isEmpty()) {
                map.remove(key);
            }
            return obj;
        } else {
            return null;
        }
    }

    private InvalidTypeException wrongTypeError() {
        return new InvalidTypeException("WRONGTYPE Operation against a key holding the wrong kind of value");
    }

    public StreamId lastId(String key) {
        Value value = map.get(key);
        if (value == null || value.getType() != Type.STREAM) return StreamId.ZERO;

        Stream stream = ((StreamValue) value).stream;
        return stream.getLastId();
    }
}
