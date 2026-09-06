package redis.commands;

import redis.infrastructure.InvalidTypeException;
import redis.store.Store;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.LRANGE;

@Component(LRANGE)
public class LRange implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (command.length != 4) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'lrange' command"));
        } else {
            try {
                String key = command[1];
                int start = Integer.parseInt(command[2]);
                int end = Integer.parseInt(command[3]);
                return Optional.of(respSerializer.serializeList(store.lrange(key, start, end)));
            } catch (NumberFormatException e) {
                return Optional.of(respSerializer.serializeError("ERR value is not an integer or out of range"));
            } catch (InvalidTypeException e) {
                return Optional.of(respSerializer.serializeError(e.getMessage()));
            }
        }
    }
}
