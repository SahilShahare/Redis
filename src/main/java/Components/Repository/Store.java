package Components.Repository;

import Components.Infrastructure.DoubleEndedList;
import Components.Infrastructure.Stream;
import Components.Infrastructure.StreamEntry;
import Components.Infrastructure.StreamId;
import Components.Infrastructure.Type;
import Components.Service.RespSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static Constants.CommandConstants.BULK_NULL;
import static Constants.CommandConstants.NULL_ARRAY;
import static Constants.CommandConstants.OK;

@Slf4j
@Component
public class Store {
    public ConcurrentHashMap<String, Value> map;

    @Autowired
    RespSerializer respSerializer;

    public Store() {
        map = new ConcurrentHashMap<>();
    }

    public Set<String> getKeys() {
        return map.keySet();
    }

    public String type(String key) {
        Value value = map.get(key);
        if (value == null) {
            return respSerializer.serializeSimpleString("none");
        } else {
            return respSerializer.serializeSimpleString(value.getType().getObjectType());
        }
    }

    public String set(String key, String val) {
        try {
            StringValue stringValue = new StringValue(val, LocalDateTime.now(), LocalDateTime.MAX);
            map.put(key, stringValue);
            return OK;
        } catch (Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String set(String key, String val, int expiryMilliSeconds) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime exp = now.plus(expiryMilliSeconds, ChronoUnit.MILLIS);
            StringValue stringValue = new StringValue(val, now, exp);
            map.put(key, stringValue);
            return OK;
        } catch (Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String get(String key) {
        try {
            LocalDateTime now = LocalDateTime.now();
            Value value = map.get(key);
            if(value == null) {
                return BULK_NULL;
            }
            if (value.getType() == Type.STRING) {
                StringValue stringValue = (StringValue) value;
                if (stringValue.expiry.isBefore(now)) {
                    map.remove(key);
                    return BULK_NULL;
                } else {
                    return respSerializer.serializeBulkString((String) stringValue.val);
                }
            } else {
                return wrongTypeError();
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String rpush(String key, List<Object> values) {
        try {
            Value value = map.computeIfAbsent(key, k -> new ListValue());
            if (value.getType() == Type.LIST) {

                DoubleEndedList<Object> lst = ((ListValue) value).list;
                int n;
                if (values.size() == 1) {
                    n = lst.addRight(values.getFirst());
                } else {
                    n = lst.addAllRight(values);
                }
                return respSerializer.serializeInteger(n);
            } else {
                return wrongTypeError();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String lpush(String key, List<Object> values) {
        try {
            Value value = map.computeIfAbsent(key, k -> new ListValue());
            if (value.getType() == Type.LIST) {
                DoubleEndedList<Object> lst = ((ListValue) value).list;
                int n;
                if (values.size() == 1) {
                    n = lst.addLeft(values.getFirst());
                } else {
                    n = lst.addAllLeft(values);
                }
                return respSerializer.serializeInteger(n);
            } else {
                return wrongTypeError();

            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String lrange(String key, int start, int end) {
        Value value = map.get(key);
        if (value == null) {
            return respSerializer.serializeList(new ArrayList<>());
        }

        if (value.getType() == Type.LIST) {
            try {
                DoubleEndedList<Object> lst = ((ListValue) value).list;
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
        } else {
            return wrongTypeError();
        }
    }

    public String llen(String key) {
        Value value = map.get(key);
        if (value == null) {
            return respSerializer.serializeInteger(0);
        }

        if (value.getType() == Type.LIST) {
            try {
                DoubleEndedList<Object> lst = ((ListValue) value).list;
                if (lst == null) {
                    return respSerializer.serializeInteger(0);
                } else {
                    return respSerializer.serializeInteger(lst.size());
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                return BULK_NULL;
            }
        } else {
            return wrongTypeError();
        }
    }

    public String lpop(String key, int count) {
        Value value = map.get(key);
        if (value == null) {
            return BULK_NULL;
        }

        if (value.getType() == Type.LIST) {
            DoubleEndedList<Object> lst = ((ListValue) value).list;

            if (lst == null) return BULK_NULL;

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
        } else {
            return wrongTypeError();
        }
    }

    public String xadd(String key, String idSpec, LinkedHashMap<String, String> fields) {
        Value value = map.computeIfAbsent(key, k -> new StreamValue());

        if (value.getType() == Type.STREAM) {
            try {
                Stream stream = ((StreamValue) value).stream;
                return respSerializer.serializeBulkString(stream.xadd(idSpec, fields));
            } catch (IllegalArgumentException e) {
                return respSerializer.serializeError(e.getMessage());
            }
        } else {
            return wrongTypeError();
        }
    }

    public String xrange(String key, String startSpec, String endSpec) {
        Value value = map.get(key);
        if(value == null) {
            return NULL_ARRAY;
        }
        if(value.getType() == Type.STREAM) {
            Stream stream = ((StreamValue) value).stream;
            return respSerializer.serializeStreamEntries(stream.xrange(startSpec, endSpec));
        } else {
            return wrongTypeError();
        }
    }

    public String xread(List<String> keys, List<String> ids) {
        List<String> parts = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);

            Value value = map.get(key);
            if (value == null) {
                continue;
            }

            if(value.getType() == Type.STREAM) {
                Stream stream = ((StreamValue) value).stream;
                List<StreamEntry> entries = stream.xread(ids.get(i));
                if (!entries.isEmpty()) {
                    parts.add(respSerializer.serializeStreamReadEntry(key, entries));
                }
            } else {
                return wrongTypeError();
            }
        }

        if (parts.isEmpty()) {
            return NULL_ARRAY;
        }
        StringBuilder res = new StringBuilder("*").append(parts.size()).append("\r\n");
        parts.forEach(res::append);
        return res.toString();
    }

    //Non-command methods

    public Object pollListLeft(String key) {
        Value value = map.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof ListValue) {
            DoubleEndedList<Object> lst = ((ListValue) value).list;
            if (lst == null) {
                return null;
            }
            return lst.removeLeft();
        } else {
            return null;
        }
    }

    private String wrongTypeError() {
        return respSerializer
                .serializeBulkString("WRONGTYPE Operation against a key holding the wrong kind of value");
    }

    public StreamId lastId(String key) {
        Value value = map.get(key);
        if(value == null || value.getType()!=Type.STREAM) return StreamId.ZERO;

        Stream stream = ((StreamValue) value).stream;
        return stream.getLastId();
    }
}
