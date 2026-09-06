package redis.commands;

import redis.infrastructure.InvalidTypeException;
import redis.store.Store;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.BULK_NULL;
import static constants.CommandConstants.GET;

@Component(GET)
public class Get implements RedisCommand {

    @Autowired
    Store store;

    @Autowired
    RespSerializer respSerializer;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (command.length != 2) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'get' command"));
        }
        String key = command[1];
        try {
            Optional<String> result = store.get(key);
            return Optional.of(result.map(s -> respSerializer.serializeBulkString(s)).orElse(BULK_NULL));
        } catch (InvalidTypeException e) {
            return Optional.of(respSerializer.serializeError(e.getMessage()));
        }

    }
}
