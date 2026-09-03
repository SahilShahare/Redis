package Components.Repository;

import Components.Service.RespSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static Constants.CommandConstants.BULK_NULL;
import static Constants.CommandConstants.OK;

@Slf4j
@Component
public class Store {
    public ConcurrentHashMap<String, Value> map;

    @Autowired
    RespSerializer respSerializer;

    public Store(){
        map = new ConcurrentHashMap<>();
    }

    public Set<String> getKeys(){
        return map.keySet();
    }

    public String set(String key, String val) {
        try{
            Value value = new Value(val, LocalDateTime.now(), LocalDateTime.MAX);
            map.put(key, value);
            return OK;
        } catch(Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String set(String key, String val, int expiryMilliSeconds) {
        try{
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime exp = now.plus(expiryMilliSeconds, ChronoUnit.MILLIS);
            Value value = new Value(val, now, exp);
            map.put(key, value);
            return OK;
        } catch(Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }

    public String get(String key) {
        try {
            LocalDateTime now = LocalDateTime.now();
            Value value = map.get(key);

            if(value.expiry.isBefore(now)){
                map.remove(key);
                return BULK_NULL;
            } else {
                return respSerializer.serializeBulkString(value.val);
            }
        } catch(Exception e) {
            log.error(e.getMessage());
            return BULK_NULL;
        }
    }
}
