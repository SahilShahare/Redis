package redis.commands;

import redis.handler.RespSerializer;
import redis.store.Store;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.OK;
import static constants.CommandConstants.PX;
import static constants.CommandConstants.SET;

@Component(SET)
public class Set implements RedisCommand {

    @Autowired
    Store store;

    @Autowired
    RespSerializer respSerializer;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (command.length < 3) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'set' command"));
        }

        String key = command[1];
        String value = command[2];

        int pxFlag = -1;
        for (int i = 3; i < command.length; i++) {
            if (PX.equalsIgnoreCase(command[i])) {
                pxFlag = i;
                break;
            }
        }

        if (pxFlag > -1) {
            if (pxFlag + 1 >= command.length) {
                return Optional.of(respSerializer.serializeError("ERR syntax error"));
            }
            try {
                int delta = Integer.parseInt(command[pxFlag + 1]);
                if (delta <= 0) {
                    return Optional.of(respSerializer.serializeError("ERR invalid expire time in 'set' command"));
                }
                store.set(key, value, delta);
            } catch (NumberFormatException e) {
                return Optional.of(respSerializer.serializeError("ERR value is not an integer or out of range"));
            }
        } else if (command.length > 3) {
            return Optional.of(respSerializer.serializeError("ERR syntax error"));
        } else {
            store.set(key, value);
        }
        return Optional.of(OK);
    }
}
