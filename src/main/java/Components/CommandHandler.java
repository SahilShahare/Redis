package Components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CommandHandler {

    private static final String PONG = "+PONG\r\n";

    @Autowired
    public RespSerializer respSerializer;

    @Autowired
    public Store store;

    public String ping(String[] command){
        return PONG;
    }

    public String echo(String[] command) {
       return respSerializer.serializeBulkString(command[1]);
    }

    public String set(String[] command) {
        try {
            String key = command[1];
            String value = command[2];

            int pxFlag = Arrays.stream(command).toList().indexOf("px");

            if(pxFlag > -1) {
                int delta = Integer.parseInt(command[pxFlag + 1]);
                return store.set(key, value, delta);
            } else {
                return store.set(key, value);
            }

        } catch (Exception e) {
            return "$-1\r\n";
        }
    }

    public String get(String[] command) {

        try{
            String key = command[1];
            return store.get(key);

        } catch(Exception e){
            return "$-1\r\n";
        }
    }
}
